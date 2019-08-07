/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.ext.impl;

import android.content.Context;

import androidx.annotation.NonNull;

import com.cyberlink.facemedemo.sdk.FaceHolder;

/**
 * TODO: Implement extra logic yourself.
 */
public class AutoAddFaceCollectionHandlerImpl extends AbsAutoAddFaceCollectionHandlerImpl {

    public AutoAddFaceCollectionHandlerImpl(@NonNull Context context) {
        super(context);
    }

    @Override
    public void put(float confidenceThreshold, FaceHolder faceHolder) {
        addNewFaceCollection(faceHolder);
    }
}
