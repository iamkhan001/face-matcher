/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.ext.impl;

import android.content.Context;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.cyberlink.faceme.FaceMeDataManager;
import com.cyberlink.faceme.FaceMeRecognizer;
import com.cyberlink.faceme.SimilarFaceResult;
import com.cyberlink.facemedemo.ext.AutoAddFaceCollectionHandler;
import com.cyberlink.facemedemo.ext.FaceExtraHandler;
import com.cyberlink.facemedemo.sdk.FaceHolder;

import java.util.List;
import java.util.Map;

public abstract class AbsFaceExtraHandlerImpl implements FaceExtraHandler {

    private final Context appContext;
    private final AutoAddFaceCollectionHandler autoAddFaceCollectionHandler;

    private FaceMeDataManager dataManager = null;

    private transient boolean enableAutoInsertUnknownFace = false;

    AbsFaceExtraHandlerImpl(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.autoAddFaceCollectionHandler = AutoAddFaceCollectionHandler.Factory.create(context);
    }

    @Override
    public void reset() {
        autoAddFaceCollectionHandler.reset();
        dataManager = null;
    }

    @Override
    public void setFaceMeRecognizer(FaceMeRecognizer recognizer) {
        this.autoAddFaceCollectionHandler.setFaceMeRecognizer(recognizer);
    }

    @Override
    public void setFaceMeDataManager(FaceMeDataManager dataManager) {
        this.dataManager = dataManager;
        this.autoAddFaceCollectionHandler.setFaceMeDataManager(dataManager);
    }

    @Override
    public void setEnableAutoInsertUnknownFace(boolean enable) {
        this.enableAutoInsertUnknownFace = enable;
    }

    @Override
    public Long processFaceHolders(float confidenceThreshold, @NonNull List<FaceHolder> faceHolders) {
        Long tsRecognize = null;

        for (FaceHolder faceHolder : faceHolders) {
            Long timestamp = searchFaceCollection(confidenceThreshold, faceHolder);
            if (timestamp != null) {
                if (tsRecognize == null) tsRecognize = 0L;
                tsRecognize += timestamp;
            }
        }

        return tsRecognize;
    }

    @CallSuper
    @Override
    public boolean isFilterOut(FaceHolder faceHolder) {
        if (faceHolder.faceBitmap == null) {
            throw new IllegalStateException("Need face Bitmap");
        }

        return false;
    }

    @Override
    public Long searchFaceCollection(float confidenceThreshold, FaceHolder faceHolder) {
        if (isFilterOut(faceHolder)) return null;

        Long tsRecognize = null;
        if (faceHolder.faceFeature != null) {
            List<SimilarFaceResult> searchResult = dataManager.searchSimilarFace(confidenceThreshold, -1, faceHolder.faceFeature);
            if (searchResult != null && !searchResult.isEmpty()) {
                for (SimilarFaceResult result : searchResult) {
                    if (result.confidence > faceHolder.data.confidence) {
                        faceHolder.data.collectionId = result.collectionId;
                        faceHolder.data.faceId = result.faceId;
                        faceHolder.data.confidence = result.confidence;
                    }
                }
            }

            //noinspection unchecked
            Map<String, Integer> dataStatus = (Map)dataManager.getProperty("Performance");
            if (dataStatus != null) {
                if (dataStatus.containsKey("Recognize")) tsRecognize = (long)dataStatus.get("Recognize");
            }

            if (faceHolder.data.collectionId > 0) {
                faceHolder.data.name = dataManager.getFaceCollectionName(faceHolder.data.collectionId);
            } else if (enableAutoInsertUnknownFace) {
                autoAddFaceCollectionHandler.put(confidenceThreshold, faceHolder);
            }
        }

        return tsRecognize;
    }
}
