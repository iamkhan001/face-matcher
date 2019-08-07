/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.validate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.cyberlink.facemedemo.ui.BaseActivity;
import com.cyberlink.facemedemo.ui.CLToast;
import com.cyberlink.facemedemo.ui.LicenseInfoHandler;
import com.cyberlink.facemedemo.ui.SettingsFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mirobotic.facematcher.BuildConfig;
import com.mirobotic.facematcher.R;
import com.mirobotic.facematcher.utils.ASFUriHelper;

import java.io.File;
import java.util.Locale;

public class ValidationActivity extends BaseActivity {
    private static final String TAG = "ValidationActivity";

    private static final int FILE_REQUEST_CODE = 400;

    private File validationFile;
    private ActionBar actionBar;

    private ValidationTask validationTask = null;

    @Override
    protected String getTagId() {
        return TAG;
    }

    @LayoutRes
    @Override
    protected int getContentLayout() {
        return R.layout.activity_validation;
    }

    @Override
    protected void initialize() {
        // Initialize UI components
        initUiComponents();
    }

    @Override
    protected String[] getRequiredPermissions() {
        return new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE };
    }

    @Override
    protected String getPermissionString() {
        return "Storage";
    }

    @Override
    protected void onPermissionsGranted() {
        resetValidationTaskItems();
    }

    private void initUiComponents() {
        initToolBar();
        initFloatingActionButton();
        initTaskStatView();
        initValidationTaskItems();
    }

    private void initToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_validation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menuChooseFile:
                clearTaskStat();
                openFileChooser();
                return true;
            case R.id.menuSettings:
                clearTaskStat();
                SettingsFragment.newInstance(false, false).show(getSupportFragmentManager());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            File file = ASFUriHelper.getFile(this, data);
            if (file == null || !file.exists()) {
                CLToast.show(this, "File is unavailable. Choose another one.");
                return;
            }

            validationFile = file;
            uiSettings.setValidationFile(file);
            resetValidationTaskItems();
        }
    }

    private void initFloatingActionButton() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((view) -> {
            LicenseInfoHandler.getLicenseInfo(this, (licenseInfo) -> {
                runValidation();
            });
        });
    }

    private TextView txtValidationMaster, txtValidationDetail;
    private void initTaskStatView() {
        txtValidationMaster = findViewById(R.id.txtValidationMaster);
        txtValidationDetail = findViewById(R.id.txtValidationDetail);
    }
    private void clearTaskStat() {
        if (validationTask != null) {
            validationTask.cancel();
            validationTask = null;
            CLToast.show(this, "Running task is cancelled.");
        }

        txtValidationMaster.setText("\n\n");
        txtValidationDetail.setText("");
        txtValidationMaster.setVisibility(View.INVISIBLE);

        countOfTotalTasks = countOfPass = countOfFail = countOfError = countOfSkip = 0;
    }

    private final int colorInfo = Color.rgb(0x00, 0x99, 0xFF);
    private final int colorWarn = Color.rgb(0xFF, 0x99, 0x00);
    private final int colorPass = Color.GREEN;
    private final int colorError = Color.RED;

    private int countOfTotalTasks = 0;
    private int countOfPass = 0;
    private int countOfFail = 0;
    private int countOfError = 0;
    private int countOfSkip = 0;

    private String toPercentage(int numerator) {
        float percentage = 0;
        if (countOfTotalTasks > 0) {
            percentage = numerator * 100F / countOfTotalTasks;
        }
        return ", " + String.format(Locale.US, "%.1f", percentage) + "%";
    }

    private void summarizeTasks() {
        String msgTotal = "Total:" + countOfTotalTasks + toPercentage(countOfPass + countOfFail + countOfError + countOfSkip) + "\n";
        Spannable spanTotal = new SpannableString(msgTotal);
        spanTotal.setSpan(new ForegroundColorSpan(colorInfo), 0, msgTotal.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        String msgPass = "Pass:" + countOfPass + toPercentage(countOfPass) + "; ";
        Spannable spanPass = new SpannableString(msgPass);
        spanPass.setSpan(new ForegroundColorSpan(colorPass), 0, msgPass.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        String msgFail = "Fail:" + countOfFail + toPercentage(countOfFail) + "\n";
        Spannable spanFail = new SpannableString(msgFail);
        spanFail.setSpan(new ForegroundColorSpan(colorError), 0, msgFail.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        String msgError = "Error:" + countOfError + toPercentage(countOfError) + "; ";
        Spannable spanError = new SpannableString(msgError);
        spanError.setSpan(new ForegroundColorSpan(colorError), 0, msgError.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        String msgSkip = "Skip:" + countOfSkip + toPercentage(countOfSkip);
        Spannable spanSkip = new SpannableString(msgSkip);
        spanSkip.setSpan(new ForegroundColorSpan(colorWarn), 0, msgSkip.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        runOnUiThread(() -> {
            txtValidationMaster.setText(spanTotal);
            txtValidationMaster.append(spanPass);
            txtValidationMaster.append(spanFail);
            txtValidationMaster.append(spanError);
            txtValidationMaster.append(spanSkip);
        });
    }

    private Observer observer = new Observer() {
        private long timestamp;

        private void showVisibleUi() {
            runOnUiThread(() -> {
                txtValidationMaster.setVisibility(View.VISIBLE);
                summarizeTasks();
            });
        }

        private void append(int color, String content) {
            append(color, 1F, content);
        }
        private void append(int color, float scale, String content) {
            String msg = content + "\n";
            Spannable s = new SpannableString(msg);
            if (scale != 1F) s.setSpan(new RelativeSizeSpan(scale), 0, msg.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new ForegroundColorSpan(color), 0, msg.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            runOnUiThread(() -> txtValidationDetail.append(s));
        }

        private String format(BaseTask task) {
            return format(task.lineNum, task.getTaskName());
        }
        private String format(int lineNum, String name) {
            return "  [" + lineNum + "][" + name + "] ";
        }

        private boolean showDetail() {
            // UI would hang if too many content append into TextView.
            return countOfTotalTasks <= 500;
        }

        @Override
        public void onInfo(String msg) {
            append(colorInfo, 1.2F, msg);
        }

        @Override
        public void onVerbose(String msg) {
            Log.v(TAG, "  " + msg);
            if (showDetail()) append(Color.GRAY, 0.7F, "  " + msg);
        }

        @Override
        public void onParseBegin() {
            timestamp = System.currentTimeMillis();
            onInfo("Begin to parse tasks...");
            onVerbose(validationFile.getAbsolutePath());
        }

        @Override
        public void onInit(int lineNum, String name) {
            Log.d(TAG, format(lineNum, name) + "init");
        }

        @Override
        public void onWarning(int lineNum, String name, String msg) {
            Log.w(TAG, format(lineNum, name) + msg);
            append(colorWarn, format(lineNum, name) + msg);
        }

        @Override
        public void onParseComplete(int countOfTasks) {
            countOfTotalTasks = countOfTasks;
            countOfPass = countOfFail = countOfError = countOfSkip = 0;
            showVisibleUi();

            onInfo("Complete parsing " + countOfTasks + " tasks, took " + (System.currentTimeMillis() - timestamp) + "ms");
        }

        @Override
        public void onWarning(BaseTask task, String msg) {
            Log.w(TAG, format(task) + msg);
            append(colorWarn, format(task) + msg);
        }

        @Override
        public void onSkip(BaseTask task) {
            countOfSkip++;
            summarizeTasks();
        }

        @Override
        public void onResult(BaseTask task, boolean isPass, String answerFromFile, String answerFromEngine) {
            if (isPass) {
                countOfPass++;
                Log.i(TAG, format(task) + "PASS");
                if (showDetail()) append(colorPass, format(task) + "PASS");
            } else {
                countOfFail++;
                String reason = "A:" + answerFromFile + " != Q:" + answerFromEngine;
                Log.e(TAG, format(task) + reason);
                if (showDetail()) append(colorError, format(task) + reason);
            }
            summarizeTasks();
        }

        @Override
        public void onError(BaseTask task, String msg) {
            countOfError++;
            summarizeTasks();

            if (!TextUtils.isEmpty(msg)) {
                Log.e(TAG, format(task) + msg);
                if (showDetail()) append(colorError, format(task) + msg);
            }
        }

        @Override
        public void onParseFailed(String msg) {
            append(colorError, msg);
        }

        @Override
        public void onValidateBegin() {
            timestamp = System.currentTimeMillis();
            onInfo("Begin to validate tasks...");
        }
        @Override
        public void onValidateComplete(File outputFile) {
            onInfo("Complete tasks validation, took " + (System.currentTimeMillis() - timestamp) + "ms");
            runOnUiThread(() -> openOutputFile(outputFile));
            validationTask = null;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (validationTask != null) validationTask.cancel();
    }

    private void initValidationTaskItems() {
        if (hasRequiredPermissions()) resetValidationTaskItems();
    }

    private void resetValidationTaskItems() {
        if (!hasRequiredPermissions()) return;

        if (validationFile == null) {
            validationFile = uiSettings.getValidationFile();
        }
        if (validationFile.exists()) {
            actionBar.setTitle(validationFile.getName());

        } else {
            validationFile = null;
            actionBar.setTitle(R.string.app_name);
        }

        clearTaskStat();
    }

    private void runValidation() {
        if (!hasRequiredPermissions()) return;
        if (validationFile == null) {
            CLToast.show(this, "Validation file is unavailable. Please choose one.");
            return;
        }

        clearTaskStat();
        validationTask = new ValidationTask(this, uiSettings, observer);
        validationTask.start();
    }

    private void openOutputFile(File outputFile) {
        Context context = getApplicationContext();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Uri outputUri = FileProvider.getUriForFile(context,
                BuildConfig.APPLICATION_ID + ".provider", outputFile);
        intent.setDataAndType(outputUri, "text/csv");

        PackageManager pm = context.getPackageManager();
        if (intent.resolveActivity(pm) != null) {
            startActivity(intent);
        }
    }
}
