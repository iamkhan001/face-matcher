/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.camera.custom;

import android.app.Activity;
import android.hardware.Camera;

import com.cyberlink.facemedemo.util.CustomDevice;

/**
 * For CyberLink used only to customize different configurations for different hardware devices.
 *
 * This handling is not for general Android devices. Even the same parameters would
 * work on one device, and might be useless, even harmful to other devices.
 */
public interface CustomHandler {

    default void applyAppOrientation(Activity activity) {}

    default void applyParameters(Camera.Parameters parameters) {}

    default Integer forceBitmapRotation() { return null; }

    default Integer forceCameraDisplayOrientation() { return null; }

    default Integer getUiLogicalCameraNum() { return null; }

    class Factory {
        public static CustomHandler create() {
            CustomDevice device  = CustomDevice.get();

            switch (device) {
                case ACER_ABS_3G1:  return new AcerAbs3G1Handler();
                case ALTEK_AQ360:   return new AltekAq360Handler();
                default:            return new CustomHandler() {};
            }
        }
    }
}
