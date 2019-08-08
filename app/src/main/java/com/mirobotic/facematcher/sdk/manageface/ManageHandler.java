/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.manageface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.cyberlink.faceme.FaceMeDataManager;
import com.cyberlink.faceme.QueryResult;
import com.cyberlink.facemedemo.sdk.FaceMeRecognizerWrapper;
import com.cyberlink.facemedemo.ui.CLToast;
import com.cyberlink.facemedemo.ui.LicenseInfoHandler;

import java.util.ArrayList;

class ManageHandler {
    private static final String TAG = "ManageHandler";

    interface ManageFaceHandlerCallback {
        void onInitEngine(boolean success);
    }

    private final Context context;
    private final Handler backgroundHandler;
    private final Handler mainHandler;
    private FaceMeDataManager faceMeDataManager = null;
    private ManageFaceHandlerCallback callback;

    ManageHandler(@NonNull Context context, @NonNull ManageFaceHandlerCallback callback) {
        this.context = context;
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.backgroundHandler = new Handler(thread.getLooper());
        mainHandler = new Handler(Looper.getMainLooper());
        this.callback = callback;

        LicenseInfoHandler.getLicenseInfo(context, (licenseInfo) -> backgroundHandler.post(() -> {
            initEngine();
        }));
    }

    @WorkerThread
    private void initEngine() {
        try {
            releaseEngine();

            byte[] licenseInfo = LicenseInfoHandler.getLicenseInfo();
            boolean success;
            long start = System.currentTimeMillis();

            FaceMeRecognizerWrapper faceMeRecognizer = new FaceMeRecognizerWrapper(context);
            // 1st: LicenseManager.
            faceMeRecognizer.registerLicense();

            // 2nd: FaceMeRecognizer.
            faceMeRecognizer.initialize();

            // 3rd: FaceMeDataManager.
            FaceMeDataManager faceMeDataManager = new FaceMeDataManager(context);
            success = faceMeDataManager.initialize(faceMeRecognizer.getFeatureScheme(), licenseInfo);
            if (!success) throw new IllegalStateException("Initialize data manager failed");

            // Always profiling in demo app.
            success = faceMeDataManager.setProperty("Profiling", true);
            if (!success) throw new IllegalStateException("Profiling data manager failed");

            //Because manageFaceHandler only need faceMeDataManager, release faceMeRecognizer after use featureScheme to init faceMeDataManager.
            faceMeRecognizer.release();

            this.faceMeDataManager = faceMeDataManager;
            mainHandler.post(() -> callback.onInitEngine(true));
            Log.v(TAG, " > initEngine took " + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            Log.e(TAG, "Cannot setup FaceMe components", e);
            CLToast.showLong(context, "Cannot setup FaceMe components\n" + e.getMessage());
            releaseEngine();
            mainHandler.post(() -> callback.onInitEngine(false));
        }
    }

    void releaseEngine() {
        if (faceMeDataManager != null) {
            faceMeDataManager.release();
            faceMeDataManager = null;
        }
    }

    ArrayList<Long> getAllFaceCollections() {
        ArrayList<Long> collectionIds = new ArrayList<>();
        if (faceMeDataManager != null) {
            long index = 0;
            int pageSize = 50;
            QueryResult result = null;
            do {
                result = faceMeDataManager.queryFaceCollection(index, pageSize);
                if (result == null) break;
                collectionIds.addAll(result.resultIds);
                index = result.nextIndex;
            } while (index != -1);
        }
        return collectionIds;
    }

    CollectionHolder getCollectionInfo(long collectionId) {
        if (faceMeDataManager != null) {
            String name = faceMeDataManager.getFaceCollectionName(collectionId);
            ArrayList<Long> faceIds = getFaceIdsByCollectionId(collectionId);
            CollectionHolder collectionHolder = new CollectionHolder(collectionId, name, faceIds);
            return collectionHolder;
        }
        return null;
    }

    ArrayList<Long> getFaceIdsByCollectionId(long collectionId) {
        ArrayList<Long> faceIds = new ArrayList<>();
        if (faceMeDataManager != null) {
            long index = 0;
            int pageSize = 50;
            QueryResult result = null;
            do {
                result = faceMeDataManager.queryFace(collectionId, index, pageSize);
                if (result == null) break;
                faceIds.addAll(result.resultIds);
                index = result.nextIndex;
            } while (index != -1);
        }
        return faceIds;
    }

    boolean deleteCollection(long collectionId) {
        if (faceMeDataManager != null) {
            return faceMeDataManager.deleteFaceCollection(collectionId);
        }
        return false;
    }

    boolean deleteFace(long faceId) {
        if (faceMeDataManager != null) {
            return faceMeDataManager.deleteFace(faceId);
        }
        return false;
    }

    boolean updateCollectionName(long collectionId, String name) {
        if (faceMeDataManager != null) {
            return faceMeDataManager.setFaceCollectionName(collectionId, name);
        }
        return false;
    }

    Bitmap getFaceThumbnail(long faceId) {
        //get face bitmap from memory cache.
        Bitmap bitmap = FaceBitmapCache.getInstance().getBitmapFromMemCache(faceId);

        //fetch bitmap from db if memory cache is not hit.
        if (bitmap == null && faceMeDataManager != null) {
            byte[] faceByteArray = faceMeDataManager.getFaceCustomData(faceId);
            if (faceByteArray != null) {
                bitmap = BitmapFactory.decodeByteArray(faceByteArray, 0, faceByteArray.length);
                //save to memory cache.
                FaceBitmapCache.getInstance().addBitmapToMemoryCache(faceId, bitmap);
            }
        }
        return bitmap;
    }

}
