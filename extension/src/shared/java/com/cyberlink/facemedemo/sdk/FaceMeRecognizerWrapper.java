/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.cyberlink.faceme.DetectionModelSpeedLevel;
import com.cyberlink.faceme.DetectionOutputOrder;
import com.cyberlink.faceme.ExtractConfig;
import com.cyberlink.faceme.ExtractionOption;
import com.cyberlink.faceme.FaceAttribute;
import com.cyberlink.faceme.FaceFeature;
import com.cyberlink.faceme.FaceFeatureScheme;
import com.cyberlink.faceme.FaceInfo;
import com.cyberlink.faceme.FaceLandmark;
import com.cyberlink.faceme.FaceMeRecognizer;
import com.cyberlink.faceme.LicenseManager;
import com.cyberlink.facemedemo.data.SavedFace;
import com.cyberlink.facemedemo.extension.R;
import com.cyberlink.facemedemo.ui.LicenseInfoHandler;
import com.cyberlink.facemedemo.ui.UiSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to wrap {@link FaceMeRecognizer} to configure
 * proper arguments in one place. It's for demo purpose to
 * centralized control arguments.
 */
public class FaceMeRecognizerWrapper {
    private static final String TAG = "FaceMeSdkWrapper";

    private final Context context;
    private final UiSettings uiSettings;
    private final ExtractConfig extractConfig;
    private final boolean cropFaceBitmap;

    private final float thr = 0.80f;

    private FaceMeRecognizer recognizer = null;
    private FaceFeatureScheme featureScheme;
    private OnExtractedListener onExtractedListener = new OnExtractedListener() {};

    private final ArrayList<FaceHolder> faceHolders = new ArrayList<>();

    private ArrayList<SavedFace> savedFaces;
    private OnFaceDetect onFaceDetect;

    private OnSavedFaceListener onSavedFaceListener = new OnSavedFaceListener() {
        @Override
        public void setSavedFaces(ArrayList<SavedFace> faces) {
            savedFaces = faces;
            decodeSavedFaces();
        }

        @Override
        public ArrayList<SavedFace> getSavedFaces() {
            return savedFaces;
        }

        @Override
        public void addFace(SavedFace face) {
            savedFaces.add(face);
        }

        @Override
        public void removeFace(int index) {
            savedFaces.remove(index);
        }
    };

    public OnSavedFaceListener getOnSavedFaceListener() {
        return onSavedFaceListener;
    }


    public FaceMeRecognizerWrapper(Context context) {
        this(context, false);
    }

    public FaceMeRecognizerWrapper(Context context, boolean cropFaceBitmap) {
        this.context = context;

        this.uiSettings = new UiSettings(context);

        this.extractConfig = new ExtractConfig();
        this.extractConfig.extractBoundingBox = true;

        this.cropFaceBitmap = cropFaceBitmap;

        savedFaces = new ArrayList<>();

    }

    public void setOnFaceDetect(OnFaceDetect onFaceDetect) {
        this.onFaceDetect = onFaceDetect;
    }



    public FaceMeRecognizerWrapper(Context context, ArrayList<SavedFace> faces, boolean cropFaceBitmap) {
        this.context = context;

        this.uiSettings = new UiSettings(context);

        this.extractConfig = new ExtractConfig();
        this.extractConfig.extractBoundingBox = true;

        this.cropFaceBitmap = cropFaceBitmap;

        savedFaces = faces;

    }

    private void decodeSavedFaces() {
        Toast.makeText(context,"Face List Updated!",Toast.LENGTH_SHORT).show();

        new Thread(() -> {

            for (SavedFace face:savedFaces){
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(face.getPath());

                    if (bitmap==null){
                        continue;
                    }

                    if (recognizer == null) {
                        Log.e(TAG, "extractFace but didn't initialize yet");
                        return;
                    }

                    faceHolders.clear();
                    int facesCount = recognizer.extractFace(extractConfig, Collections.singletonList(bitmap));

                    if (facesCount > 0) {
                        for (int faceIndex = 0; faceIndex < facesCount; faceIndex++) {
                            FaceFeature faceFeature = recognizer.getFaceFeature(0, faceIndex);
                            FaceAttribute faceAttr = recognizer.getFaceAttribute(0, faceIndex);

                            face.setBitmap(bitmap);
                            face.setFeature(faceFeature);
                            face.setAttribute(faceAttr);

                            Log.e(TAG,faceIndex+" >> age "+faceAttr.age+" | gender "+faceAttr.gender+" | emotion "+faceAttr.emotion);
                        }
                    }

                    Log.e(TAG,"SAVED FACES >> "+ savedFaces.size());

                }catch (Exception e){
                    e.printStackTrace();
                }
            }



        }).start();

    }

    public void setOnExtractedListener(@NonNull OnExtractedListener onExtractedListener) {
        this.onExtractedListener = onExtractedListener;
    }

    public FaceMeRecognizer get() {
        return recognizer;
    }

    public void registerLicense() {
        byte[] licenseInfo = LicenseInfoHandler.getLicenseInfo();

        // 1st: LicenseManager.
        LicenseManager licenseManager = null;
        try {
            licenseManager = new LicenseManager(context);
            boolean success = licenseManager.initialize(licenseInfo);
            if (!success) throw new IllegalStateException("Initialize license manager failed");

            int result = licenseManager.registerLicense();
            if (result < 0) {
                throw new IllegalStateException("Register license failed: " + result);
            }
        } finally {
            if (licenseManager != null) licenseManager.release();
        }
    }

    public void initialize() {
        byte[] licenseInfo = LicenseInfoHandler.getLicenseInfo();
        boolean success;

        try {
            release();

            // 2nd: FaceMeRecognizer.
            recognizer = new FaceMeRecognizer(context);
            success = recognizer.initialize(uiSettings.getEnginePreference(), uiSettings.getEngineThreads(),
                    DetectionModelSpeedLevel.DEFAULT, uiSettings.getExtractModel(), licenseInfo);
            if (!success) throw new IllegalStateException("Initialize recognizer failed");

            // Always profiling in demo app.
            success = recognizer.setProperty("Profiling", true);
            if (!success) throw new IllegalStateException("Profiling recognizer failed");

            featureScheme = recognizer.getFeatureScheme();
            if (featureScheme == null) throw new IllegalStateException("Get feature scheme failed");


            Log.i(TAG, "Confidence threshold list");
            Log.i(TAG, " > 1e-6: " + featureScheme.threshold_1_1e6);
            Log.i(TAG, " > 1e-5: " + featureScheme.threshold_1_1e5);
            Log.i(TAG, " > 1e-4: " + featureScheme.threshold_1_1e4);
            Log.i(TAG, " > 1e-3: " + featureScheme.threshold_1_1e3);
            Log.i(TAG, " > 1e-2: " + featureScheme.threshold_1_1e2);

        } catch (Exception e) {
            release();
            throw e;
        }
    }

    public void release() {
        if (recognizer != null) {
            recognizer.release();
            recognizer = null;
        }
        featureScheme = null;
        onExtractedListener = new OnExtractedListener() {};
        faceHolders.clear();
    }

    public void configure() {
        if (recognizer == null) {
            Log.e(TAG, "configure but didn't initialize yet");
            return;
        }

        recognizer.setExtractionOption(ExtractionOption.MIN_FACE_WIDTH, uiSettings.getMinFaceWidth());
        recognizer.setExtractionOption(ExtractionOption.DETECTION_SPEED_LEVEL, uiSettings.getDetectSpeedLevel());
        recognizer.setExtractionOption(ExtractionOption.DETECTION_OUTPUT_ORDER, DetectionOutputOrder.CONFIDENCE);
        recognizer.setExtractionOption(ExtractionOption.DETECTION_MODE, uiSettings.getDetectionMode());
        // recognizer.setExtractionOption(ExtractionOption.FAST_DETECTION_PERIOD, 5000);

        extractConfig.extractFeatureLandmark = uiSettings.isShowLandmark();
        if (uiSettings.isShowFeatures()) {
            extractConfig.extractFeature = true;
            extractConfig.extractAge = uiSettings.isShowAge();
            extractConfig.extractGender = uiSettings.isShowGender();
            extractConfig.extractEmotion = uiSettings.isShowEmotion();
            extractConfig.extractPose = uiSettings.isShowPose();
        } else {
            extractConfig.extractFeature = false;
            extractConfig.extractAge = false;
            extractConfig.extractGender = false;
            extractConfig.extractEmotion = false;
            extractConfig.extractPose = false;
        }
    }

    public FaceFeatureScheme getFeatureScheme() {
        return featureScheme;
    }

    public int extractFace(Bitmap bitmap) {
        if (recognizer == null) {
            Log.e(TAG, "extractFace but didn't initialize yet");
            return -1;
        }

        faceHolders.clear();
        int facesCount = recognizer.extractFace(extractConfig, Collections.singletonList(bitmap));
        if (facesCount > 0) {
            for (int faceIndex = 0; faceIndex < facesCount; faceIndex++) {
                FaceInfo faceInfo = recognizer.getFaceInfo(0, faceIndex);
                FaceLandmark faceLandmark = recognizer.getFaceLandmark(0, faceIndex);
                FaceAttribute faceAttr = recognizer.getFaceAttribute(0, faceIndex);
                FaceFeature faceFeature = recognizer.getFaceFeature(0, faceIndex);

                Bitmap faceBitmap = null;
                if (cropFaceBitmap) {
                    long tsBitmap = System.currentTimeMillis();

                    Rect detectFaceRect = new Rect();
                    detectFaceRect.left = Math.max(faceInfo.boundingBox.left, 0);
                    detectFaceRect.top = Math.max(faceInfo.boundingBox.top, 0);
                    detectFaceRect.right = Math.min(faceInfo.boundingBox.right, bitmap.getWidth());
                    detectFaceRect.bottom = Math.min(faceInfo.boundingBox.bottom, bitmap.getHeight());
                    faceBitmap = getFaceBitmap(bitmap, detectFaceRect);

                    long duration = System.currentTimeMillis() - tsBitmap;
                    if (duration > 10) {
                        Log.v(TAG, "   [" + faceIndex + "] Bitmap face took " + duration + "ms");
                    }
                }

                boolean isFaceMatched = false;

                for (int f = 0; f< savedFaces.size(); f++){
                    SavedFace face = savedFaces.get(f);

                    face.getAttribute();

                    if (face.getAttribute()==null){
                        continue;
                    }

                    float c = compareFaceFeature(face.getFeature(),faceFeature);
                    Log.e(TAG, "face ["+faceIndex+ "] feature ["+f+"] >> result >> "+c);

                    if (c > thr){
                        if (onFaceDetect != null){
                            onFaceDetect.onFaceDetect(face);
                        }
                        isFaceMatched = true;
                        Log.d(TAG,"Face Result "+f);
                        break;
                    }

                }
                if (!isFaceMatched){
                    if (onFaceDetect != null){
                        onFaceDetect.onFaceNotFound();
                    }
                }



                FaceHolder holder = new FaceHolder(faceInfo, faceLandmark, faceAttr, faceFeature, faceBitmap);
                faceHolders.add(holder);
            }
        }else {
            if (onFaceDetect != null){
                onFaceDetect.onFaceNotFound();
            }
        }
        onExtractedListener.onExtracted(bitmap.getWidth(), bitmap.getHeight(), new ArrayList<>(faceHolders));

        return facesCount;
    }

    private Bitmap getFaceBitmap(Bitmap source, Rect detectFaceRect) {
        Size sourceSize = new Size(source.getWidth(), source.getHeight());
        // For each trained face, save the color frame (640 x 480) to the database.
        if (detectFaceRect.width() > detectFaceRect.height()) {
            int space = (detectFaceRect.width() - detectFaceRect.height()) / 2;
            detectFaceRect = ThumbnailUtil.enlargeThumbnail(detectFaceRect, sourceSize, 0, space, 0, space, false);
        } else if (detectFaceRect.width() < detectFaceRect.height()) {
            int space = -(detectFaceRect.width() - detectFaceRect.height()) / 2;
            detectFaceRect = ThumbnailUtil.enlargeThumbnail(detectFaceRect, sourceSize, space, 0, space, 0, false);
        }
        Rect enlargeRect = ThumbnailUtil.enlargeThumbnail(detectFaceRect, sourceSize, 0.25f, false);


        //adjust rect to Square
        int centerX = enlargeRect.centerX();
        int centerY = enlargeRect.centerY();
        int minEdge = Math.min(enlargeRect.width(), enlargeRect.height());
        int maxEdge = Math.max(enlargeRect.width(), enlargeRect.height());
        int squareEdge = minEdge;
        if (centerX - Math.ceil(maxEdge / 2.0) >= 0 && centerX + Math.ceil(maxEdge / 2.0) <= sourceSize.getWidth() &&
                centerY - Math.ceil(maxEdge / 2.0) >= 0 && centerY + Math.ceil(maxEdge / 2.0) <= sourceSize.getHeight()) {
            squareEdge = maxEdge;
        }
        Rect enlargeSquareFaceRect = new Rect();
        enlargeSquareFaceRect.left = Math.max(0, centerX - (int)Math.ceil(squareEdge / 2.0));
        enlargeSquareFaceRect.top = Math.max(0, centerY - (int)Math.ceil(squareEdge / 2.0));
        enlargeSquareFaceRect.right = enlargeSquareFaceRect.left + squareEdge;
        enlargeSquareFaceRect.bottom = enlargeSquareFaceRect.top + squareEdge;
        return Bitmap.createBitmap(source, enlargeSquareFaceRect.left, enlargeSquareFaceRect.top, enlargeSquareFaceRect.width(), enlargeSquareFaceRect.height());
    }

    public List<FaceHolder> getFaceHolders() {
        return new ArrayList<>(faceHolders);
    }

    public FaceHolder getFaceHolder(@IntRange(from = 0L) int faceIndex) {
        if (faceIndex < faceHolders.size()) {
            return faceHolders.get(faceIndex);
        } else {
            return null;
        }
    }

    public FaceInfo getFaceInfo(@IntRange(from = 0L) int faceIndex) {
        if (recognizer == null) {
            Log.e(TAG, "getFaceInfo but didn't initialize yet");
            return null;
        }

        return recognizer.getFaceInfo(0, faceIndex);
    }

    public FaceLandmark getFaceLandmark(@IntRange(from = 0L) int faceIndex) {
        if (recognizer == null) {
            Log.e(TAG, "getFaceLandmark but didn't initialize yet");
            return null;
        }

        return recognizer.getFaceLandmark(0, faceIndex);
    }

    public FaceAttribute getFaceAttribute(@IntRange(from = 0L) int faceIndex) {
        if (recognizer == null) {
            Log.e(TAG, "getFaceAttribute but didn't initialize yet");
            return null;
        }

        return recognizer.getFaceAttribute(0, faceIndex);
    }

    public FaceFeature getFaceFeature(@IntRange(from = 0L) int faceIndex) {
        if (recognizer == null) {
            Log.e(TAG, "getFaceFeature but didn't initialize yet");
            return null;
        }

        return recognizer.getFaceFeature(0, faceIndex);
    }

    public float compareFaceFeature(FaceFeature faceA, FaceFeature faceB) {
        if (recognizer == null) {
            Log.e(TAG, "compareFaceFeature but didn't initialize yet");
            return -1;
        }

        return recognizer.compareFaceFeature(faceA, faceB);
    }

    public boolean setProperty(String propertyId, Object value) {
        if (recognizer == null) {
            Log.e(TAG, "setProperty but didn't initialize yet");
            return false;
        }

        return recognizer.setProperty(propertyId, value);
    }

    public Object getProperty(String propertyId) {
        if (recognizer == null) {
            Log.e(TAG, "getProperty but didn't initialize yet");
            return null;
        }

        return recognizer.getProperty(propertyId);
    }

    public interface OnSavedFaceListener{
        void setSavedFaces(ArrayList<SavedFace> faces);
        ArrayList<SavedFace> getSavedFaces();
        void addFace(SavedFace face);
        void removeFace(int index);
    }

    public interface OnFaceDetect{
        void onFaceDetect(SavedFace face);
        void onFaceNotFound();
    }
}
