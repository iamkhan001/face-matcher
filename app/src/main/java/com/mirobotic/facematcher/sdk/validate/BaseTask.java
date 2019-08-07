/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.validate;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.exifinterface.media.ExifInterface;

import com.cyberlink.faceme.FaceAttribute;
import com.cyberlink.faceme.FaceFeature;
import com.cyberlink.faceme.FaceFeatureScheme;
import com.cyberlink.faceme.FaceInfo;
import com.cyberlink.faceme.PrecisionLevel;
import com.cyberlink.facemedemo.sdk.FaceHolder;
import com.cyberlink.facemedemo.sdk.FaceMeRecognizerWrapper;
import com.cyberlink.facemedemo.sdk.OnExtractedListener;
import com.cyberlink.facemedemo.ui.UiSettings;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

abstract class BaseTask {
    interface Callback {
        void onWarning(BaseTask task, String msg);
        void onSkip(BaseTask task);
        void onResult(BaseTask task, boolean isPass, String answerFromFile, String answerFromEngine);
        void onError(BaseTask task, String msg);
    }

    private final Callback callback;
    private final File rootDir;
    final int lineNum;
    final String content;
    final String[] args;

    private UiSettings uiSettings;
    private FaceMeRecognizerWrapper recognizer;
    private FaceFeatureScheme featureScheme;
    private HashMap<String, FaceHolder> faceHolders = new HashMap<>();

    private boolean canValidate;
    private boolean isCancelled = false;
    private String currentRecognizerImagePath = null;

    BaseTask(Callback callback, File rootDir, int lineNum, String content) {
        this.callback = callback;
        this.rootDir = rootDir;
        this.lineNum = lineNum;
        this.content = content;
        this.args = content.split(":", getArgCount());

        canValidate = parseArgs();
    }

    BaseTask setUiSettings(UiSettings uiSettings) {
        this.uiSettings = uiSettings;
        return this;
    }
    BaseTask setRecognizer(FaceMeRecognizerWrapper recognizer) {
        this.recognizer = recognizer;
        this.featureScheme = recognizer.getFeatureScheme();
        return this;
    }

    void cancel() {
        isCancelled = true;
    }

    private static String format(String msg, Object... args) {
        return String.format(Locale.US, msg, args);
    }

    void logW(String msg, Object... args) {
        callback.onWarning(this, format(msg, args));
    }

    abstract int getArgCount();

    String getTaskName() {
        return args[0];
    }

    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    abstract boolean parseArgs();

    final boolean prepare() {
        if (!canValidate) return false;

        return doPrepare();
    }

    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    abstract boolean doPrepare();

    /**
     * @return {@code null} means validation PASS. Otherwise, return answer from recognizer.
     */
    abstract String validate();

    /**
     * @return Answer described in validation file.
     */
    String getAnswer() {
        return String.valueOf(args[2].trim());
    }

    @Nullable
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    final File composeFile(String subPath) {
        String revisedSubPath = subPath.trim().replaceAll("\\\\", "/");
        File imageFile = new File(rootDir, revisedSubPath);
        if (imageFile.exists()) return imageFile;

        logW("media is not found: %s", subPath);
        return null;
    }

    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    final float compare(FaceFeature faceA, FaceFeature faceB) {
        return recognizer.compareFaceFeature(faceA, faceB);
    }

    private boolean recognize(String subPath) {
        if (isCancelled) return false;

        File image = composeFile(subPath);
        if (image == null) return false;

        String imagePath = image.getAbsolutePath();
        if (TextUtils.equals(imagePath, currentRecognizerImagePath)) return true;

        Bitmap bitmap = decodeBitmap(imagePath);
        if (isCancelled || bitmap == null) return false;

        recognizer.setOnExtractedListener(new OnExtractedListener() {
            @Override
            public void onExtracted(int width, int height, @NonNull List<FaceHolder> faces) {
                if (!faces.isEmpty()) {
                    faceHolders.put(subPath, faces.get(0));
                }
            }
        });

        int facesCount = recognizer.extractFace(bitmap);
        if (facesCount != 1) logW("number of faces in specific image is not 1 but %d: %s", facesCount, subPath);
        if (facesCount < 1) return false;

        currentRecognizerImagePath = imagePath;

        // Just log and warn it because some scenario would intend to be unavailable.
        if (faceHolders.get(subPath).faceInfo == null) {
            logW("FaceInfo is unavailable: %s", subPath);
        }

        if (faceHolders.get(subPath).faceAttribute == null) {
            logW("FaceAttribute is unavailable: %s", subPath);
        }

        if (faceHolders.get(subPath).faceFeature == null) {
            logW("FaceFeature is unavailable: %s", subPath);
        }

        return true;
    }

    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    protected FaceInfo getFaceInfo(String subPath) {
        File image = composeFile(subPath);
        if (image == null) return null;

        FaceHolder faceHolder = faceHolders.get(subPath);
        if (faceHolder == null || faceHolder.faceInfo == null) {
            if (!recognize(subPath)) return null;

            faceHolder = faceHolders.get(subPath);
        }
        return faceHolder.faceInfo;
    }

    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    FaceAttribute getFaceAttribute(String subPath) {
        File image = composeFile(subPath);
        if (image == null) return null;

        FaceHolder faceHolder = faceHolders.get(subPath);
        if (faceHolder == null || faceHolder.faceAttribute == null) {
            if (!recognize(subPath)) return null;

            faceHolder = faceHolders.get(subPath);
        }
        return faceHolder.faceAttribute;
    }

    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    FaceFeature getFaceFeature(String subPath) {
        File image = composeFile(subPath);
        if (image == null) return null;

        FaceHolder faceHolder = faceHolders.get(subPath);
        if (faceHolder == null || faceHolder.faceFeature == null) {
            if (!recognize(subPath)) return null;

            faceHolder = faceHolders.get(subPath);
        }
        return faceHolder.faceFeature;
    }

    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    float getPrecisionThreshold() {
        @PrecisionLevel.EPrecisionLevel int level = uiSettings.getPrecisionLevel();
        switch (level) {
            case PrecisionLevel.LEVEL_1E2:
                return featureScheme.threshold_1_1e2;
            case PrecisionLevel.LEVEL_1E3:
                return featureScheme.threshold_1_1e3;
            case PrecisionLevel.LEVEL_1E4:
                return featureScheme.threshold_1_1e4;
            case PrecisionLevel.LEVEL_1E5:
                return featureScheme.threshold_1_1e5;
            case PrecisionLevel.LEVEL_1E6:
                return featureScheme.threshold_1_1e6;
            default:
                return featureScheme.threshold_1_1e6;
        }
    }

    private Bitmap decodeBitmap(String imagePath) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            bitmap = BitmapFactory.decodeFile(imagePath, options);
            // Handle EXIF orientation.
            if ("image/jpeg".equals(options.outMimeType)) {
                bitmap = rotateJpegBitmapIfNeeded(imagePath, bitmap);
            }
        } catch (Throwable t) {
            logW("cannot decode Bitmap: " + t.getMessage(), t);
        }

        if (isCancelled && bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        return bitmap;
    }

    private static Bitmap rotateJpegBitmapIfNeeded(String imagePath, Bitmap bitmap) throws IOException {
        ExifInterface exifInterface = new ExifInterface(imagePath);
        int degrees = exifInterface.getRotationDegrees();
        if (degrees == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();

        return rotatedBitmap;
    }

}
