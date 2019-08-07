/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.view.Surface;

import java.util.concurrent.atomic.AtomicBoolean;

class FrameV17 {
    private static final String TAG = "CamCtrl.F17";

    private final BaseCameraController cameraController;
    private final CameraController.Callback cameraCallback;
    private final StatListener statListener;
    private final RenderScript rs;
    private final ScriptIntrinsicYuvToRGB scriptYuv2Rgb;
    private final Handler workerHandler;
    private final AtomicBoolean isHandlingFrame = new AtomicBoolean(false);

    private int previewWidth;
    private int previewHeight;
    private Allocation srcAllocation;
    private Allocation dstAllocation;

    private Bitmap landscapeBitmap;
    private Bitmap portraitBitmap;
    private Bitmap renderedBitmap;
    private Canvas landscapeCanvas;
    private Canvas portraitCanvas;

    FrameV17(BaseCameraController cameraController, CameraController.Callback callback, StatListener listener) {
        this.cameraController = cameraController;
        this.cameraCallback = callback;
        this.statListener = listener;
        this.rs = RenderScript.create(cameraController.appContext);
        this.scriptYuv2Rgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.workerHandler = new Handler(thread.getLooper());
    }

    void setPreviewSize(int bufferLength, int previewWidth, int previewHeight) {
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;

        workerHandler.removeCallbacksAndMessages(null);
        workerHandler.post(() -> {
            setupAllocations(bufferLength);
            setupRenderBitmaps();
        });
    }

    private void setupAllocations(int bufferLength) {
        Type.Builder srcBuilder = new Type.Builder(rs, Element.U8(rs))
                .setX(bufferLength)
                .setMipmaps(false);
        if (srcAllocation != null) srcAllocation.destroy();
        srcAllocation = Allocation.createTyped(rs, srcBuilder.create(),
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

        Type.Builder dstBuilder = new Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(previewWidth)
                .setY(previewHeight)
                .setMipmaps(false);
        if (dstAllocation != null) dstAllocation.destroy();
        dstAllocation = Allocation.createTyped(rs, dstBuilder.create(),
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
    }

    private void setupRenderBitmaps() {
        if (landscapeBitmap != null) landscapeBitmap.recycle();
        landscapeBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        landscapeCanvas = new Canvas(landscapeBitmap);

        if (portraitBitmap != null) portraitBitmap.recycle();
        //noinspection SuspiciousNameCombination
        portraitBitmap = Bitmap.createBitmap(previewHeight, previewWidth, Bitmap.Config.ARGB_8888);
        portraitCanvas = new Canvas(portraitBitmap);

        if (renderedBitmap != null) renderedBitmap.recycle();
        renderedBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
    }

    void release() {
        workerHandler.getLooper().quitSafely();
    }

    void onFrame(byte[] data, int displayOrientation, boolean isCameraFacingBack, Runnable callback) {
        if (isHandlingFrame.getAndSet(true)) {
            callback.run();
            return;
        }

        workerHandler.post(() -> {
            // Do something from data.
            handleFrame(data, displayOrientation, isCameraFacingBack);

            isHandlingFrame.set(false);
            callback.run();
        });
    }

    private void handleFrame(byte[] data, int displayOrientation, boolean isCameraFacingBack) {
        long start = System.currentTimeMillis();

        srcAllocation.copy1DRangeFromUnchecked(0, data.length, data);
        scriptYuv2Rgb.setInput(srcAllocation);
        scriptYuv2Rgb.forEach(dstAllocation);
        dstAllocation.copyTo(renderedBitmap);
        statListener.onImageCaptured();

        // Since we shared the same Bitmap between FrameV17 and client to detect Faces.
        // So we have to determine right now if need to blending to shared Bitmap.
        cameraCallback.checkTask(new CameraController.CheckCallback() {
            @Override
            public void acquired() {
                Bitmap canvasBitmap = rotateOrFlip(displayOrientation, isCameraFacingBack);
                Bitmap output = Bitmap.createBitmap(canvasBitmap);
                statListener.onBitmapCreated(System.currentTimeMillis() - start);

                cameraCallback.onBitmap(output);
            }

            @Override
            public void rejected() {}
        });
    }

    private Bitmap rotateOrFlip(int displayOrientation, boolean isCameraFacingBack) {
        long start = System.currentTimeMillis();

        int rotationDegree = getDeviceRotationDegree();

        Bitmap bitmap;
        Canvas canvas;

        if (rotationDegree == 0 || rotationDegree == 180) {
            bitmap = portraitBitmap;
            canvas = portraitCanvas;
        } else {
            bitmap = landscapeBitmap;
            canvas = landscapeCanvas;
        }

        if (canvas != null) {
            canvas.save();
            setupCanvasOrientation(rotationDegree, displayOrientation, isCameraFacingBack, canvas);
            canvas.drawBitmap(renderedBitmap, 0, 0, null);
            canvas.restore();
        }

        statListener.onBitmapRotated(System.currentTimeMillis() - start);

        return bitmap;
    }

    private void setupCanvasOrientation(int rotationDegree, int displayOrientation, boolean isCameraFacingBack, Canvas canvas) {
        float halfWidth = previewWidth / 2F;
        float halfHeight = previewHeight / 2F;

        boolean isDevicePortrait = rotationDegree == 0 || rotationDegree == 180;
        if (!isCameraFacingBack) {
            if (isDevicePortrait) {
                canvas.scale(1, -1, halfHeight, halfWidth);
            } else {
                canvas.scale(1, -1, halfWidth, halfHeight);
            }
        }

        if (isDevicePortrait) {
            canvas.translate(halfHeight, halfWidth);
        }

        if (isDevicePortrait) {
            canvas.rotate(displayOrientation - rotationDegree);
        } else if (isCameraFacingBack && rotationDegree == 270 || !isCameraFacingBack && rotationDegree == 90) {
            canvas.rotate(180, halfWidth, halfHeight);
        }

        if (isDevicePortrait) {
            canvas.translate(-halfWidth, -halfHeight);
        }
    }

    private int getDeviceRotationDegree() {
        Integer forceRotateDegrees = cameraController.forceRotateDegrees;
        if (forceRotateDegrees != null) return forceRotateDegrees;

        int rotation = cameraController.getDeviceRotation();
        switch (rotation) {
            case Surface.ROTATION_90:   return 90;
            case Surface.ROTATION_180:  return 180;
            case Surface.ROTATION_270:  return 270;
            case Surface.ROTATION_0:
            default:
                return 0;
        }
    }
}
