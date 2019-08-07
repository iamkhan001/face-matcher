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
import com.cyberlink.facemedemo.ext.impl.FaceExtraHandlerImpl;
import com.cyberlink.facemedemo.sdk.FaceHolder;

import java.util.List;

public interface FaceExtraHandler {
    class Factory {
        public static FaceExtraHandler create(@NonNull Context context) {
            return new FaceExtraHandlerImpl(context);
        }
    }

    void reset();

    void setFaceMeRecognizer(FaceMeRecognizer recognizer);

    void setFaceMeDataManager(FaceMeDataManager dataManager);

    void setEnableAutoInsertUnknownFace(boolean enable);

    Long processFaceHolders(float confidenceThreshold, @NonNull List<FaceHolder> faceHolders);

    boolean isFilterOut(FaceHolder faceHolder);

    Long searchFaceCollection(float confidenceThreshold, FaceHolder faceHolder);
}
