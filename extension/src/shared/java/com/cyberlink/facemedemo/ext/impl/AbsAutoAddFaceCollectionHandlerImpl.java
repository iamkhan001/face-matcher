/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.ext.impl;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.cyberlink.faceme.FaceMeDataManager;
import com.cyberlink.faceme.FaceMeRecognizer;
import com.cyberlink.facemedemo.ext.AutoAddFaceCollectionHandler;
import com.cyberlink.facemedemo.sdk.FaceHolder;
import com.cyberlink.facemedemo.util.BmpUtil;

public abstract class AbsAutoAddFaceCollectionHandlerImpl implements AutoAddFaceCollectionHandler {

    private final Context appContext;

    FaceMeRecognizer recognizer = null;
    FaceMeDataManager dataManager = null;

    AbsAutoAddFaceCollectionHandlerImpl(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void reset() {
        recognizer = null;
        dataManager = null;
    }

    @Override
    public void setFaceMeRecognizer(FaceMeRecognizer recognizer) {
        this.recognizer = recognizer;
    }

    @Override
    public void setFaceMeDataManager(FaceMeDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public abstract void put(float confidenceThreshold, FaceHolder faceHolder);

    void addNewFaceCollection(FaceHolder faceHolder) {
        long collectionId = dataManager.createFaceCollection("User#auto");
        dataManager.setFaceCollectionName(collectionId, "User#" + collectionId);

        long faceId = dataManager.addFace(collectionId, faceHolder.faceFeature);
        if (faceHolder.faceBitmap == null) return;

        Bitmap scaledFaceBitmap = Bitmap.createScaledBitmap(faceHolder.faceBitmap, 96, 96, true);
        if (scaledFaceBitmap != null) {
            byte faceByteArray[] = BmpUtil.getBmpByteArray(scaledFaceBitmap);
            if (faceByteArray != null) {
                dataManager.setFaceCustomData(faceId, faceByteArray);
            }
        }
    }
}
