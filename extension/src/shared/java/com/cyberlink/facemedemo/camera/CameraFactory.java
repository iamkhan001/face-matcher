/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.camera;

import android.app.Activity;
import android.os.Build;

import com.cyberlink.facemedemo.ui.AutoFitTextureView;

public class CameraFactory {

    // We keep CameraV1 implementation since CameraV2 has bad preview performance once add extra
    // preview Surface. Need to resolve it with OpenGL ES 2.0 implementation.
    private static final boolean FORCE_CAMERA_1 = true;

    public static CameraController create(Activity activity, AutoFitTextureView textureView, CameraController.Callback callback, StatListener listener) {
        if (FORCE_CAMERA_1) {
            return new CameraV17(activity, textureView, callback, listener);
        } else {
            return new CameraV21(activity, textureView, callback, listener);
        }
    }
}
