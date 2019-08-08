/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.cyberlink.faceme.LicenseManager;
import com.cyberlink.facemedemo.data.Config;
import com.cyberlink.facemedemo.util.Callback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A demo purpose handler to persist license information in file for next
 * time app launched to reuse.
 * For security concern, you SHOULD NOT persist your own license information
 * in any places except your product.
 */
public class LicenseInfoHandler {
    /**
     * An license information from CyberLink licensing.
     */
    private static byte[] LICENSE_INFO = null;

    private static boolean isEmptyLicenseInfo() {
        return LICENSE_INFO == null || LICENSE_INFO.length == 0;
    }

    public static byte[] getLicenseInfo() {
        if (isEmptyLicenseInfo())
            throw new IllegalStateException("You must input your own API Key first!");
        else
            return LICENSE_INFO;
    }

    public static void getLicenseInfo(Context context, @NonNull Callback<byte[]> callback) {
        if (!isEmptyLicenseInfo()) {
            callback.onCallback(LICENSE_INFO);
            return;
        }

        LICENSE_INFO = readFromFile();
        if (!isEmptyLicenseInfo()) {
            callback.onCallback(LICENSE_INFO);
            return;
        }

        //askUserInput(context, callback);

        registerLicense(context);

    }

    private static byte[] readFromFile() {
        // CAUTION: It's lazy task. It will access I/O on main thread.
        File licenseFile = new File(Environment.getExternalStorageDirectory(), "FaceMe" + File.separator + "faceme.key");
        if (!licenseFile.exists() || licenseFile.length() == 0) return null;

        byte[] licenseInfo;
        try {
            try (FileInputStream is = new FileInputStream(licenseFile)) {
                int bufferSize = 0x20000; // ~130K.
                byte[] buffer = new byte[bufferSize];
                ByteArrayOutputStream os = new ByteArrayOutputStream(bufferSize);
                int read;
                while (true) {
                    read = is.read(buffer);
                    if (read == -1) break;

                    os.write(buffer, 0, read);
                }
                licenseInfo = os.toByteArray();
                os.close();
            }
        } catch (IOException ignored) {
            licenseInfo = null;
        }

        return licenseInfo;
    }

    private static void writeToFile(byte[] licenseInfo) {
        // CAUTION: It's lazy task. It will access I/O on main thread.
        File licenseFileDir = new File(Environment.getExternalStorageDirectory(), "FaceMe");
        if (!licenseFileDir.exists()) licenseFileDir.mkdirs();

        File licenseFile = new File(licenseFileDir, "faceme.key");
        try {
            try (FileOutputStream os = new FileOutputStream(licenseFile)) {
                os.write(licenseInfo);
            }
        } catch (IOException ignored) {}
    }

    private static void askUserInput(Context context, Callback<byte[]> callback) {
        EditText editText = new EditText(context);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        // editText.setText("", TextView.BufferType.EDITABLE);

        new AlertDialog.Builder(context)
                .setTitle("Input FaceMe SDK API Key")
                .setView(editText)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    LICENSE_INFO = editText.getText().toString().trim().getBytes();
                    if (isEmptyLicenseInfo()) {
                        CLToast.show(context, "You must have a valid API key to use FaceMe SDK");
                    } else {
                        writeToFile(LICENSE_INFO);
                        callback.onCallback(LICENSE_INFO);
                    }
                })
                .setNeutralButton("Cancel", (dialog, which) -> {
                    CLToast.show(context, "You must have a valid API key to use FaceMe SDK");
                })
                .show();
    }

    private static void registerLicense(Context context) {
        LicenseManager licenseManager = new LicenseManager(context);
        boolean isRegistered = licenseManager.initialize(Config.API_KEY);
        int result = licenseManager.registerLicense();
        Log.e("LicenseManager","Result "+isRegistered+" >> "+result);

    }
}
