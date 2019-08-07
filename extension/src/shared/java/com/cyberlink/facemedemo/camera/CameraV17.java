/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.camera;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.cyberlink.facemedemo.extension.R;
import com.cyberlink.facemedemo.ui.AutoFitTextureView;
import com.cyberlink.facemedemo.ui.CLToast;

import java.util.List;

/**
 * Use Camera1 for better preview performance.
 */
@SuppressWarnings("deprecation")
class CameraV17 extends BaseCameraController {
    private static final String TAG = "CamCtrl.V17";

    private final Camera.PreviewCallback onReceivedPreviewFrame;
    private final Handler cameraHandler;
    private final FrameV17 frameHandler;

    private int cameraId;
    private Camera camera;
    private int displayOrientation;
    private byte[][] mFrameBuffer = new byte[2][]; // Keep 2~3 frames.

    CameraV17(Activity activity, AutoFitTextureView surfaceView, Callback callback, StatListener listener) {
        super(activity, surfaceView, callback, listener);

        this.onReceivedPreviewFrame = newPreviewCallback();
        this.frameHandler = new FrameV17(this, callback, listener);

        cameraId = Camera.getNumberOfCameras() > 1 ? 1 : 0;

        HandlerThread cameraThread = new HandlerThread(TAG);
        cameraThread.start();
        this.cameraHandler = new Handler(cameraThread.getLooper());
    }

    private Camera.PreviewCallback newPreviewCallback() {
        return (data, camera) -> {
            statListener.onFrameCaptured();

            frameHandler.onFrame(data, displayOrientation, isCameraFacingBack, () -> camera.addCallbackBuffer(data));
        };
    }

    @Override
    public int getUiLogicalCameraNum() {
        Integer num = customHandler.getUiLogicalCameraNum();
        if (num != null) return num;

        return Camera.getNumberOfCameras();
    }

    @Override
    void startCamera(boolean nextCameraId) {
        super.startCamera(nextCameraId);

        if (!isTextureAvailable.get() || !mTextureView.isAvailable()) {
            Log.w(TAG, " > texture is unavailable yet");
            return;
        }
        if (noCameraPermission()) {
            CLToast.show(appContext, R.string.ext_permission_fail, "Camera");
            return;
        }

        adjustTextureViewAspectRatio();
        configureTransform(); // startCamera

        cameraHandler.post(() -> startCameraInternal(nextCameraId));
    }

    @WorkerThread
    private void startCameraInternal(boolean nextCameraId) {
        try {
            if (nextCameraId) cameraId = getNextCameraId();
            camera = Camera.open(cameraId);
            if (camera == null) throw new IllegalStateException("Camera open but null returned");

            int facing = getCameraInfo(cameraId).facing;
            boolean facingBack = facing == Camera.CameraInfo.CAMERA_FACING_BACK;
            if (isCameraFacingBack != facingBack) {
                Log.w(TAG, " > request " + (isCameraFacingBack ? "back" : "front") + " but got another");
                isCameraFacingBack = facingBack;
            }

            setupCameraParameters();

        } catch (Exception e) {
            Log.e(TAG, " > cannot open camera", e);
            if (camera != null) {
                camera.release();
                camera = null;
            }

            CLToast.show(appContext, R.string.ext_launcher_no_camera_available);
        }
    }

    private static Camera.CameraInfo getCameraInfo(int cameraId) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        return cameraInfo;
    }

    @Override
    public List<Camera.Size> getResolutions() {
        try {
            return camera == null ? null : camera.getParameters().getSupportedPreviewSizes();
        } catch (Exception e) {
            CLToast.showLong(appContext, "Cannot list all resolutions");
            return null;
        }
    }

    private int getNextCameraId() {
        int num = Camera.getNumberOfCameras();
        if (num == 0) throw new IllegalStateException("Hardware camera is unavailable");

        cameraId += 1;
        if (cameraId >= num) cameraId = 0;
        return cameraId;

//        int facing = isCameraFacingBack ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
//        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
//        for (int cameraId = 0; cameraId < num; cameraId++) {
//            Camera.getCameraInfo(cameraId, cameraInfo);
//            if (cameraInfo.facing == facing) {
//                Log.d(TAG, " > getCameraId: " + cameraId + ", #" + num);
//                return cameraId;
//            }
//        }
//        return 0;
    }

    private void setupCameraParameters() {
        try {
            Camera.Parameters params = camera.getParameters();
            params.setPreviewFormat(ImageFormat.NV21);
            params.setPreviewSize(previewWidth, previewHeight);

            setupOrientation();
            setAutoFocusModeIfPossible(params);

            customHandler.applyParameters(params);
            camera.setParameters(params);

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewWidth, previewHeight);
            camera.setPreviewTexture(texture);

            setPreviewCallbackAndBuffer(previewWidth, previewHeight);

            camera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, " > setup params but failed", e);
        }
    }

    private static void setAutoFocusModeIfPossible(Camera.Parameters parameters) {
        List<String> cameraFocusModes = parameters.getSupportedFocusModes();
        if (cameraFocusModes != null) {
            for (String focusMode : cameraFocusModes) {
                // FOCUS_MODE_CONTINUOUS_VIDEO works for recording but not preview.
                if (Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(focusMode)) {
                    parameters.setFocusMode(focusMode);
                    return;
                }
            }
            for (String focusMode : cameraFocusModes) {
                if (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(focusMode);
                    return;
                }
            }
        }
    }

    private void setPreviewCallbackAndBuffer(int width, int height) {
        int bufferLength = getBufferLength(width, height);
        initPreviewCallbackWithBuffer(bufferLength);
        frameHandler.setPreviewSize(bufferLength, width, height);

        camera.setPreviewCallbackWithBuffer(null);
        for (byte[] aFrameBuffer : mFrameBuffer) {
            camera.addCallbackBuffer(aFrameBuffer);
        }
        camera.setPreviewCallbackWithBuffer(onReceivedPreviewFrame);
    }

    private static int getBufferLength(int width, int height) {
        int yStride = (int) Math.ceil(width / 16.0) * 16;
        int uvStride = (int) Math.ceil((yStride / 2) / 16.0) * 16;
        int ySize = yStride * height;
        int uvSize = uvStride * height;
        return ySize + uvSize;
    }

    private void initPreviewCallbackWithBuffer(int bufferLength) {
        for (int i = 0; i < mFrameBuffer.length; i++) {
            mFrameBuffer[i] = new byte[bufferLength];
        }
    }

    private void setupOrientation() {
        Integer forceDisplayOrientation = customHandler.forceCameraDisplayOrientation();
        if (forceDisplayOrientation == null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                displayOrientation = 360 - info.orientation;
            } else {  // back-facing
                displayOrientation = info.orientation;
            }
        } else {
            displayOrientation = forceDisplayOrientation;
        }

        camera.setDisplayOrientation(displayOrientation);
    }

    @Override
    void stopCamera() {
        Log.d(TAG, "stopCamera");

        cameraHandler.post(this::stopCameraInternal);
    }

    private void stopCameraInternal() {
        if (camera == null) return;

        try {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
        } catch (Exception e) {
            Log.e(TAG, " > stopCamera but failed", e);
        } finally {
            camera = null;
        }
    }

    @Override
    public void release() {
        super.release();

        cameraHandler.getLooper().quitSafely();
        frameHandler.release();
    }
}
