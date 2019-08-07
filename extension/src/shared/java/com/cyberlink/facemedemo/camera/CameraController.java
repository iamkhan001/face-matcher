/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.camera;

import android.graphics.Bitmap;

import java.util.List;

public interface CameraController {
    interface Callback {
        /**
         * Callback when retrieved a frame. It would be on arbitrary thread
         * depending on implementation. Client have to recycle Bitmap itself
         * or else OutOfMemoryError would occur.
         */
        void onBitmap(Bitmap bitmap);

        /**
         * Check if client is processing previous task or available.
         */
        void checkTask(CheckCallback callback);
    }

    interface CheckCallback {
        void acquired();
        void rejected();
    }

    int getUiLogicalCameraNum();

    /**
     * Notify controller that device orientation changed.
     */
    void rotate();

    /**
     * Notify controller that Activity onResume event invoked.
     * This will trigger controller to create and open camera instance.
     */
    void resume();

    /**
     * Notify controller that Activity onPause event invoked.
     * This will trigger controller to stop and release camera instance.
     */
    void pause();

    /**
     * Notify controller that Activity onDestroy event invoked.
     * This will trigger controller to release all related resources.
     */
    void release();

    /**
     * Notify controller to switch camera between front and rear one.
     *
     * @return {@code true} means rear. Otherwise front camera.
     */
    boolean switchCamera();

    /**
     * Force to rotate Bitmap before feed into FaceRecognizer engine.
     *
     * @param forceRotateDegrees It should be 0, 90, 180, or 270. {@code null} means auto.
     */
    void forceBitmapRotation(Integer forceRotateDegrees);
    Integer getForceBitmapRotation();

    <S> List<S> getResolutions();

    void setResolution(int width, int height);
}
