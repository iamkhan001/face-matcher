/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import androidx.annotation.CallSuper;

import com.cyberlink.facemedemo.camera.custom.CustomHandler;
import com.cyberlink.facemedemo.ui.AutoFitTextureView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class BaseCameraController implements CameraController {
    private static final String TAG = "CameraController";

    final Context appContext;
    private final WindowManager windowManager;

    final AutoFitTextureView mTextureView;
    private final TextureView.SurfaceTextureListener textureCallback;
    final AtomicBoolean isTextureAvailable = new AtomicBoolean(false);

    final Callback cameraCallback;
    final StatListener statListener;
    final CustomHandler customHandler;

    /**
     * Current camera is facing_back or facing_front.
     */
    boolean isCameraFacingBack = false;

    private int viewWidth;
    private int viewHeight;
    int preferWidth = 1280; // preferSize, bufferSize.
    int preferHeight = 720;
    int previewWidth = preferWidth;
    int previewHeight = preferHeight;

    float preferFps = 30;

    BaseCameraController(Activity activity, AutoFitTextureView textureView, Callback callback, StatListener listener) {
        this.appContext = activity.getApplicationContext();
        this.windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
        this.mTextureView = textureView;
        this.textureCallback = newTextureCallback();
        this.cameraCallback = callback;
        this.statListener = listener;
        this.customHandler = CustomHandler.Factory.create();

        initComponents(activity);
    }

    private TextureView.SurfaceTextureListener newTextureCallback() {
        return new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                Log.i(TAG, "onSurfaceTextureAvailable: " + width + "x" + height);
                isTextureAvailable.set(true);
                viewWidth = width;
                viewHeight = height;
                startCamera(false);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                Log.i(TAG, "onSurfaceTextureSizeChanged: " + width + "x" + height);
                viewWidth = width;
                viewHeight = height;
                configureTransform(); // onSurfaceTextureSizeChanged
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                Log.v(TAG, "onSurfaceTextureDestroyed");
                isTextureAvailable.set(false);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            }
        };
    }

    private void initComponents(Activity activity) {
        customHandler.applyAppOrientation(activity);
        forceRotateDegrees = customHandler.forceBitmapRotation();

        mTextureView.setKeepScreenOn(true);
        mTextureView.setSurfaceTextureListener(textureCallback);
    }

    boolean noCameraPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                appContext.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     */
    void configureTransform() {
        Log.v(TAG, " > configureTransform");
        int rotation = getDeviceRotation();

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            //noinspection SuspiciousNameCombination
            RectF bufferRect = new RectF(0, 0, previewHeight, previewWidth);
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());

            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / previewHeight, (float) viewWidth / previewWidth);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(90 * rotation, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    int getDeviceRotation() {
        return (windowManager == null) ? Surface.ROTATION_0 : windowManager.getDefaultDisplay().getRotation();
    }

    void adjustTextureViewAspectRatio() {
        int orientation = appContext.getResources().getConfiguration().orientation;
        String strOrientation = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? "landscape" :
                ((orientation == Configuration.ORIENTATION_PORTRAIT) ? "portrait" : "undefined");
        Log.v(TAG, " > orientation: " + strOrientation);
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mTextureView.setAspectRatio(previewWidth, previewHeight);
        } else {
            //noinspection SuspiciousNameCombination
            mTextureView.setAspectRatio(previewHeight, previewWidth);
        }
    }

    static void logE(Exception e) {
        Log.e(TAG, "Something went wrong", e);
    }

    static void logE(String msg) {
        try {
            // Throw an Exception to print calling stack.
            throw new Exception(msg);
        } catch (Exception e) {
            logE(e);
        }
    }

    @Override
    public void setResolution(int preferWidth, int preferHeight) {
        this.previewWidth = this.preferWidth = preferWidth;
        this.previewHeight = this.preferHeight = preferHeight;

        restartCamera(false);
    }

    @CallSuper
    void startCamera(boolean nextCameraId) {
        Log.i(TAG, "startCamera");
        statListener.reset();
    }

    abstract void stopCamera();

    private void restartCamera(boolean nextCameraId) {
        Log.i(TAG, "restartCamera");
        stopCamera();
        startCamera(nextCameraId);
    }

    @Override
    public void rotate() {
        Log.d(TAG, "rotate");
        adjustTextureViewAspectRatio();
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause");

        stopCamera();
    }

    @Override
    public void resume() {
        Log.d(TAG, "resume");

        if (mTextureView.isAvailable()) startCamera(false);
    }

    @CallSuper
    @Override
    public void release() {
        Log.d(TAG, "release");

        stopCamera();
    }

    @Override
    public boolean switchCamera() {
        isCameraFacingBack = !isCameraFacingBack;
        restartCamera(true);
        return isCameraFacingBack;
    }

    Integer forceRotateDegrees = null;
    @Override
    public void forceBitmapRotation(Integer forceRotateDegrees) {
        this.forceRotateDegrees = forceRotateDegrees;
    }

    @Override
    public Integer getForceBitmapRotation() {
        return forceRotateDegrees;
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            logE("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
