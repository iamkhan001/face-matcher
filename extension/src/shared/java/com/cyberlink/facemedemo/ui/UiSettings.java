/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Size;

import androidx.annotation.NonNull;

import com.cyberlink.faceme.DetectionMode;
import com.cyberlink.faceme.DetectionSpeedLevel;
import com.cyberlink.faceme.EnginePreference;
import com.cyberlink.faceme.ExtractionModelSpeedLevel;
import com.cyberlink.faceme.PrecisionLevel;

import java.io.File;

public final class UiSettings {

    private static final String PREFERENCE_NAME = "com.cyberlink.FaceMe.Settings";

    private static final String benchmarkFolder = "benchmarkFolder";
    private static final String validationFile = "validationFile";
    private static final String showInfo = "showInfo";

    private static final String width = "width";
    private static final String height = "height";

    private static final String enginePreference = "v3_enginePreference";
    private static final String engineThreads = "engineThreads";
    private static final String extractModel = "v3_extractModel";

    private static final String minFaceWidth = "minFaceWidth";
    private static final String detectSpeedLevel = "v3_detectSpeedLevel";
    private static final String detectionMode = "detectionMode";
    private static final String precisionLevel = "v3_precisionLevel";

    private static final String showLandmark = "showLandmark";
    private static final String showFeatures = "showFeatures";
    private static final String showAge = "showAge";
    private static final String ageInRange = "ageInRange";
    private static final String showGender = "showGender";
    private static final String showEmotion = "showEmotion";
    private static final String showPose = "showPose";

    private final SharedPreferences pref;

    public UiSettings(Context context) {
        this.pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isDebugMode() {
        File debugFile = new File(Environment.getExternalStorageDirectory(), "FaceMe/debug");
        return debugFile.exists();
    }

    private void setString(String key, String value) {
        pref.edit().putString(key, value).apply();
    }
    private void setInteger(String key, int value) {
        pref.edit().putInt(key, value).apply();
    }
    private void setBoolean(String key, boolean value) {
        pref.edit().putBoolean(key, value).apply();
    }

    public void setBenchmarkFolder(@NonNull File folder) {
        setString(benchmarkFolder, folder.getAbsolutePath());
    }
    public File getBenchmarkFolder() {
        if (pref.contains(benchmarkFolder)) {
            return new File(pref.getString(benchmarkFolder, ""));
        } else {
            return new File(Environment.getExternalStorageDirectory(), "FaceMe");
        }
    }

    public void setValidationFile(@NonNull File file) {
        setString(validationFile, file.getAbsolutePath());
    }
    public File getValidationFile() {
        if (pref.contains(validationFile)) {
            return new File(pref.getString(validationFile, ""));
        } else {
            return new File(Environment.getExternalStorageDirectory(), "FaceMe/ValidationTest.txt");
        }
    }

    public void setShowInfo(boolean enable) {
        setBoolean(showInfo, enable);
    }
    public boolean isShowInfo() {
        return pref.getBoolean(showInfo, false);
    }

    public void setPreviewSize(int width, int height) {
        setInteger(UiSettings.width, width);
        setInteger(UiSettings.height, height);
    }
    public Size getPreviewSize() {
        return new Size(pref.getInt(width, 1280), pref.getInt(height, 720));
    }

    public void setEnginePreference(@EnginePreference.EEnginePreference int value) {
        setInteger(enginePreference, value);
    }
    @EnginePreference.EEnginePreference
    public int getEnginePreference() {
        return pref.getInt(enginePreference, EnginePreference.PREFER_NONE);
    }

    public void setMinFaceWidth(int value) {
        setInteger(minFaceWidth, value);
    }
    public int getMinFaceWidth() {
        return pref.getInt(minFaceWidth, 112);
    }

    public void setEngineThreads(int value) {
        setInteger(engineThreads, value);
    }
    public int getEngineThreads() {
        int cpuCounts = Runtime.getRuntime().availableProcessors();
        int defaultValue = Math.min(cpuCounts, 4);
        return pref.getInt(engineThreads, defaultValue);
    }

    public void setPrecisionLevel(int value) {
        setInteger(precisionLevel, value);
    }
    @PrecisionLevel.EPrecisionLevel
    public int getPrecisionLevel() {
        return pref.getInt(precisionLevel, PrecisionLevel.LEVEL_1E6);
    }

    public void setDetectSpeedLevel(@DetectionSpeedLevel.EDetectionSpeedLevel int value) {
        setInteger(detectSpeedLevel, value);
    }
    @DetectionSpeedLevel.EDetectionSpeedLevel
    public int getDetectSpeedLevel() {
        return pref.getInt(detectSpeedLevel, DetectionSpeedLevel.PRECISE);
    }

    public void setExtractModel(@ExtractionModelSpeedLevel.EExtractionModelSpeedLevel int value) {
        setInteger(extractModel, value);
    }
    @ExtractionModelSpeedLevel.EExtractionModelSpeedLevel
    public int getExtractModel() {
        return pref.getInt(extractModel, ExtractionModelSpeedLevel.HIGH);
    }

    public void setDetectionMode(@DetectionMode.EDetectionMode int value) {
        setInteger(detectionMode, value);
    }
    @DetectionMode.EDetectionMode
    public int getDetectionMode() {
        return pref.getInt(detectionMode, DetectionMode.FAST);
    }

    public void setShowLandmark(boolean enable) {
        setBoolean(showLandmark, enable);
    }
    public boolean isShowLandmark() {
        return pref.getBoolean(showLandmark, false);
    }

    public void setShowFeatures(boolean enable) {
        setBoolean(showFeatures, enable);
    }
    public boolean isShowFeatures() {
        return pref.getBoolean(showFeatures, true);
    }

    public void setShowAge(boolean enable) {
        setBoolean(showAge, enable);
    }
    public boolean isShowAge() {
        return pref.getBoolean(showAge, true);
    }

    public void setAgeInRange(boolean enable) {
        setBoolean(ageInRange, enable);
    }
    public boolean isAgeInRange() {
        return pref.getBoolean(ageInRange, true);
    }

    public void setShowGender(boolean enable) {
        setBoolean(showGender, enable);
    }
    public boolean isShowGender() {
        return pref.getBoolean(showGender, true);
    }

    public void setShowEmotion(boolean enable) {
        setBoolean(showEmotion, enable);
    }
    public boolean isShowEmotion() {
        return pref.getBoolean(showEmotion, true);
    }

    public void setShowPose(boolean enable) {
        setBoolean(showPose, enable);
    }
    public boolean isShowPose() {
        return pref.getBoolean(showPose, true);
    }
}
