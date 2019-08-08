/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.camerastream;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.cyberlink.faceme.FaceMeDataManager;
import com.cyberlink.faceme.QueryResult;
import com.cyberlink.facemedemo.camera.StatListener;
import com.cyberlink.facemedemo.ext.FaceExtraHandler;
import com.cyberlink.facemedemo.sdk.FaceHolder;
import com.cyberlink.facemedemo.sdk.FaceMeRecognizerWrapper;
import com.cyberlink.facemedemo.sdk.OnExtractedListener;
import com.cyberlink.facemedemo.ui.CLToast;
import com.cyberlink.facemedemo.ui.LicenseInfoHandler;
import com.cyberlink.facemedemo.ui.UiSettings;
import com.cyberlink.facemedemo.util.BmpUtil;
import com.mirobotic.facematcher.ui.activities.CameraActivity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RecognitionHandler  {
    private static final String TAG = "RecognitionHandler";
    private static final int FACE_THUMBNAIL_SIZE  = 96;

    public interface RecognizeListener {
        void onRecognized(int width, int height, List<FaceHolder> faces);
    }

    private final Context context;
    private final UiSettings uiSettings;
    private final StatListener statListener;
    private final RecognizeListener recognizeListener;
    private final OnExtractedListener extractListener;

    private final Handler recognitionHandler;
    private final Handler mainHandler;
    private final FaceExtraHandler faceExtraHandler;

    private final AtomicBoolean isRecognizing = new AtomicBoolean(false);
    private final AtomicReference<Bitmap> bitmapQueue = new AtomicReference<>(null);

    private FaceMeRecognizerWrapper faceMeRecognizer = null;
    private FaceMeDataManager faceMeDataManager = null;
    private CameraActivity.OnRecognitionHandlerReady onRecognitionHandlerReady;
    private FaceMeRecognizerWrapper.OnFaceDetect onFaceDetect;



    public RecognitionHandler(Context context, UiSettings uiSettings, StatListener statListener,
                              RecognizeListener recognizeListener, OnExtractedListener extractListener,
                              CameraActivity.OnRecognitionHandlerReady onRecognitionHandlerReady,
                              FaceMeRecognizerWrapper.OnFaceDetect onFaceDetect) {
        this.context = context;
        this.uiSettings = uiSettings;
        this.statListener = statListener;
        this.recognizeListener = recognizeListener;
        this.extractListener = extractListener;
        this.onRecognitionHandlerReady = onRecognitionHandlerReady;
        this.onFaceDetect = onFaceDetect;

        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.recognitionHandler = new Handler(thread.getLooper());
        this.mainHandler = new Handler(Looper.getMainLooper());

        this.faceExtraHandler = FaceExtraHandler.Factory.create(context);

    }

    @WorkerThread
    private void recognizeBitmap() {
        Bitmap bitmap = bitmapQueue.getAndSet(null);
        if (bitmap == null) return;

        if (faceMeRecognizer == null) {
            bitmap.recycle();
            return;
        }

        long start = System.currentTimeMillis();

        isRecognizing.set(true);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int facesCount = faceMeRecognizer.extractFace(bitmap);

        //noinspection unchecked
        Map<String, Integer> status = (Map) faceMeRecognizer.getProperty("Performance");
        int total = 0, normalDetect = 0, fastDetect = 0, extract = 0, recognize = 0;
        boolean gotProfiling = false;

        if (status != null) {
            gotProfiling = true;
            if (status.containsKey("Total")) total = status.get("Total");
            if (status.containsKey("FaceDetect")) normalDetect = status.get("FaceDetect");
            if (status.containsKey("FastFaceDetect")) fastDetect = status.get("FastFaceDetect");
            if (status.containsKey("Extract")) extract = status.get("Extract");
        }

        List<FaceHolder> faces = faceMeRecognizer.getFaceHolders();
        float confidenceThreshold = faceMeDataManager.getPrecisionThreshold(uiSettings.getPrecisionLevel());
        if (facesCount > 0) {
            long tsFaceData = System.currentTimeMillis();

            Long tsRecognize = faceExtraHandler.processFaceHolders(confidenceThreshold, faces);
            if (tsRecognize != null) {
                gotProfiling = true;
                recognize += tsRecognize;
            }

            long faceDataDuration = System.currentTimeMillis() - tsFaceData;
            if (faceDataDuration > 5) {
                Log.v(TAG, " > Search faces data took " + faceDataDuration + "ms");
            }

            extractListener.onExtracted(width, height, faces);
        }
        if (gotProfiling) {
            statListener.onFacesRecognized(facesCount, total, normalDetect, fastDetect, extract, recognize);
        }

        bitmap.recycle();
        mainHandler.post(() -> recognizeListener.onRecognized(width, height, faces));
        isRecognizing.set(false);

        Log.d(TAG, " > recognize took " + (System.currentTimeMillis() - start) + "ms");
    }

    public void applySettings(boolean needRebuild) {
        LicenseInfoHandler.getLicenseInfo(context, (licenseInfo) -> recognitionHandler.post(() -> {
            if (needRebuild) releaseEngine();
            if (faceMeRecognizer == null) initEngine();

            if (faceMeRecognizer != null) {
                faceMeRecognizer.configure();
            }
        }));
    }

    public FaceMeRecognizerWrapper.OnSavedFaceListener getOnSavedFaceListener(){
        if (faceMeRecognizer!=null){
            return faceMeRecognizer.getOnSavedFaceListener();
        }
        return null;
    }

    @WorkerThread
    private void initEngine() {
        try {
            releaseEngine();

            byte[] licenseInfo = LicenseInfoHandler.getLicenseInfo();
            boolean success;
            long start = System.currentTimeMillis();

            faceMeRecognizer = new FaceMeRecognizerWrapper(context, true);
            // 1st: LicenseManager.
            faceMeRecognizer.registerLicense();

            // 2nd: FaceMeRecognizer.
            faceMeRecognizer.initialize();

            // 3rd: FaceMeDataManager.
            faceMeDataManager = new FaceMeDataManager(context);
            success = faceMeDataManager.initialize(faceMeRecognizer.getFeatureScheme(), licenseInfo);
            if (!success) throw new IllegalStateException("Initialize data manager failed");

            // Always profiling in demo app.
            success = faceMeDataManager.setProperty("Profiling", true);
            if (!success) throw new IllegalStateException("Profiling data manager failed");

            faceExtraHandler.setFaceMeRecognizer(faceMeRecognizer.get());
            faceExtraHandler.setFaceMeDataManager(faceMeDataManager);


            onRecognitionHandlerReady.setOnSavedFaceListener(faceMeRecognizer.getOnSavedFaceListener());

            faceMeRecognizer.setOnFaceDetect(onFaceDetect);

            Log.v(TAG, " > initEngine took " + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            Log.e(TAG, "Cannot setup FaceMe components", e);
            CLToast.showLong(context, "Cannot setup FaceMe components\n" + e.getMessage());
            releaseEngine();
        }
    }

    @WorkerThread
    private void releaseEngine() {
        if (faceMeRecognizer != null) {
            faceMeRecognizer.release();
            faceMeRecognizer = null;
        }
        if (faceMeDataManager != null) {
            faceMeDataManager.release();
            faceMeDataManager = null;
            faceExtraHandler.reset();
        }
    }

    public boolean isRecognizing() {
        return this.isRecognizing.get();
    }

    public void onResume() {
        applySettings(true);
    }

    public void onPause() {
        recognitionHandler.removeCallbacksAndMessages(null);
        recognitionHandler.post(this::releaseEngine);
    }

    public void onBitmap(Bitmap newBitmap) {
        if (isRecognizing.get()) {
            newBitmap.recycle();
            return;
        }
        Bitmap oldBitmap = bitmapQueue.getAndSet(newBitmap);
        if (oldBitmap != null) {
            oldBitmap.recycle();
        }

        recognitionHandler.post(this::recognizeBitmap);
    }

    public void onRelease() {
        recognitionHandler.removeCallbacksAndMessages(null);
        recognitionHandler.post(() -> {
            releaseEngine();
            recognitionHandler.getLooper().quitSafely();
        });
    }

    void updateFace(FaceHolder faceHolder, String newName) {
        Log.d(TAG, "addFace: " + newName);
        FaceMeDataManager dataManager = faceMeDataManager;
        if (dataManager == null) {
            Log.w(TAG, " > addFace: data manager unavailable");
            return;
        }

        // Suppose collection name is unique in demo purpose.
        long collectionId;
        QueryResult queryCollectionResult = dataManager.queryFaceCollectionByName(newName, 0, 1);
        if (queryCollectionResult == null || queryCollectionResult.resultIds.isEmpty()) {
            collectionId = dataManager.createFaceCollection(newName);
            Log.v(TAG, " > addFace: create new collection: " + collectionId);
        } else {
            collectionId = queryCollectionResult.resultIds.get(0);
            Log.v(TAG, " > addFace: collection found: " + collectionId);
        }

        long faceId = dataManager.addFace(collectionId, faceHolder.faceFeature);
        if (faceId <= 0) {
            Log.e(TAG, " > addFace: but failed");
            return;
        }

        Bitmap scaledFaceBitmap = Bitmap.createScaledBitmap(faceHolder.faceBitmap, FACE_THUMBNAIL_SIZE, FACE_THUMBNAIL_SIZE, true);
        if (scaledFaceBitmap != null) {
            byte faceByteArray[] = BmpUtil.getBmpByteArray(scaledFaceBitmap);
            if (faceByteArray != null) {
                dataManager.setFaceCustomData(faceId, faceByteArray);
            }
        }

        Log.v(TAG, " > addFace: faceId: " + faceId);
        faceHolder.data.collectionId = collectionId;
        faceHolder.data.faceId = faceId;
        faceHolder.data.name = newName;
        faceHolder.data.confidence = 1F;
    }

    public void removeFace(FaceHolder faceHolder) {
        Log.d(TAG, "removeFace");
        FaceMeDataManager dataManager = faceMeDataManager;
        if (dataManager == null) {
            Log.w(TAG, " > removeFace[" + faceHolder.data.faceId + "] data manager unavailable");
            return;
        }

        boolean success = dataManager.deleteFace(faceHolder.data.faceId);
        if (success) {
            Log.v(TAG, " > removeFace[" + faceHolder.data.faceId + "] successfully");
        } else {
            Log.e(TAG, " > removeFace[" + faceHolder.data.faceId + "] failed");
        }
    }

    void enableAutoAddFace(boolean enable) {
        FaceExtraHandler extraHandler = faceExtraHandler;
        if (extraHandler == null) {
            Log.w(TAG, " > enableAutoAddFace but faceExtraHandler is unavailable");
            return;
        }

        extraHandler.setEnableAutoInsertUnknownFace(enable);
    }

    float getConfidenceThreshold() {
        Log.d(TAG, "getConfidenceThreshold");
        FaceMeDataManager dataManager = faceMeDataManager;
        if (dataManager == null) {
            Log.w(TAG, " > getConfidenceThreshold but data manager unavailable");
            return 0F;
        }

        return dataManager.getPrecisionThreshold(uiSettings.getPrecisionLevel());
    }
}
