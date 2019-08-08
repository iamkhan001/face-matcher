/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.ui;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cyberlink.facemedemo.extension.R;

public abstract class BaseActivity extends AppCompatActivity implements SettingsFragment.Broker {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int APP_SETTING_REQUEST_CODE = 200;

    protected UiSettings uiSettings;

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        long start = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        setContentView(getContentLayout());


        uiSettings = new UiSettings(this);
        // Request necessary permission.
        requestPermissionsIfNeeded();
        initialize();

        Log.d(getTagId(), "onCreate took " + (System.currentTimeMillis() - start) + "ms");
    }

    protected abstract String getTagId();

    @LayoutRes
    protected abstract int getContentLayout();

    protected abstract void initialize();

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return; // Lower Android OS, no need.
        if (hasRequiredPermissions()) return; // Has granted, no need.

        requestPermissions(getRequiredPermissions(), PERMISSION_REQUEST_CODE);
    }

    protected final boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true; // Lower Android OS, always true.

        boolean allGranted = true;
        for (String permission : getRequiredPermissions()) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        return allGranted;
    }

    @CallSuper
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != APP_SETTING_REQUEST_CODE) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if (hasRequiredPermissions()) {
            onPermissionsGranted();
        } else {
            requestPermissionsIfNeeded();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) {
            onPermissionsGranted();
            return;
        }

        showDialogForPermission();
    }

    private void showDialogForPermission() {
        String msg = getString(R.string.ext_permission_fail, getPermissionString());
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton("Go to App Settings", (dialog, which) -> openAppSettingPage())
                .setNegativeButton("Close", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void openAppSettingPage() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", getPackageName(), null));
        try {
            startActivityForResult(intent, APP_SETTING_REQUEST_CODE);
        } catch (ActivityNotFoundException t) {
            finish();
        }
    }

    protected abstract String[] getRequiredPermissions();

    protected abstract String getPermissionString();

    protected void onPermissionsGranted() {}

    @Override
    public final UiSettings getUiSettings() {
        return uiSettings;
    }


}
