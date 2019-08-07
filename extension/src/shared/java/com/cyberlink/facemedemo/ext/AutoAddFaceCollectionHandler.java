/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.ext;

import android.content.Context;

import androidx.annotation.NonNull;

import com.cyberlink.faceme.FaceMeDataManager;
import com.cyberlink.faceme.FaceMeRecognizer;
import com.cyberlink.facemedemo.ext.impl.AutoAddFaceCollectionHandlerImpl;
import com.cyberlink.facemedemo.sdk.FaceHolder;

public interface AutoAddFaceCollectionHandler {
    class Factory {
        public static AutoAddFaceCollectionHandler create(@NonNull Context context) {
            return new AutoAddFaceCollectionHandlerImpl(context);
        }
    }

    void reset();

    void setFaceMeRecognizer(FaceMeRecognizer recognizer);

    void setFaceMeDataManager(FaceMeDataManager dataManager);

    void put(float confidenceThreshold, FaceHolder faceHolder);
}
