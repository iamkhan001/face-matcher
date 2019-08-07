/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.camera.custom;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;

public class AltekAq360Handler implements CustomHandler {

    @Override
    public void applyAppOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void applyParameters(Camera.Parameters parameters) {
        parameters.set("zsl", "on");
        parameters.setFocusMode("manual");
        parameters.set("AL3200_SCID", "2");
        parameters.set("enable-raw-stream", 1);
    }

    @Override
    public Integer forceCameraDisplayOrientation() {
        return 90;
    }

    @Override
    public Integer getUiLogicalCameraNum() {
        return 1;
    }
}
