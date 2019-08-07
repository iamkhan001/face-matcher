/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.util;

import android.os.Build;
import android.util.Log;

public enum CustomDevice {
    ACER_ABS_3G1() {
        @Override
        public boolean matches() {
            return "acer".equals(Build.BRAND) &&
                    "ABS-3G1".equals(Build.MODEL) &&
                    "abs3g1_ww_gen1s".equals(Build.PRODUCT);
        }
    },

    ALTEK_AQ360() {
        @Override
        public boolean matches() {
            return "AQ360".equals(Build.MODEL) &&
                    "msm8953_64".equals(Build.DEVICE) &&
                    "ACSQBuild-01".equals(Build.HOST);
        }
    },

    // Put GENERIC to the last position.
    GENERIC() {
        @Override
        public boolean matches() {
            return true;
        }
    };

    public abstract boolean matches();

    private static final String TAG = "FaceMe.Custom";

    public static CustomDevice get() {
        for (CustomDevice device : CustomDevice.values()) {
            if (device.matches()) return device;
        }

        return GENERIC;
    }

    static {
        Log.v(TAG, "board: " + Build.BOARD);
        Log.v(TAG, "bootloader: " + Build.BOOTLOADER);
        Log.v(TAG, "brand: " + Build.BRAND);
        Log.v(TAG, "device: " + Build.DEVICE);
        Log.v(TAG, "display: " + Build.DISPLAY);
        Log.v(TAG, "fingerprint: " + Build.FINGERPRINT);
        Log.v(TAG, "hardware: " + Build.HARDWARE);
        Log.v(TAG, "host: " + Build.HOST);
        Log.v(TAG, "id: " + Build.ID);
        Log.v(TAG, "manufacturer: " + Build.MANUFACTURER);
        Log.v(TAG, "model: " + Build.MODEL);
        Log.v(TAG, "product: " + Build.PRODUCT);
        Log.v(TAG, "tags: " + Build.TAGS);
        Log.v(TAG, "type: " + Build.TYPE);
        Log.v(TAG, "user: " + Build.USER);
        Log.v(TAG, "version.release: " + Build.VERSION.RELEASE);
        Log.v(TAG, "version.sdk_int: " + Build.VERSION.SDK_INT);

    }
}
