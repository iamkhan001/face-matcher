/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.sdk;

import android.graphics.Bitmap;

import com.cyberlink.faceme.FaceAttribute;
import com.cyberlink.faceme.FaceFeature;
import com.cyberlink.faceme.FaceInfo;
import com.cyberlink.faceme.FaceLandmark;

public final class FaceHolder {

    public final FaceInfo faceInfo;
    public final FaceLandmark faceLandmark;
    public final FaceAttribute faceAttribute;
    public final FaceFeature faceFeature;
    public final Bitmap faceBitmap;

    public final FaceData data = new FaceData();

    public FaceHolder(FaceInfo faceInfo, FaceLandmark faceLandmark, FaceAttribute faceAttribute, FaceFeature faceFeature, Bitmap faceBitmap) {
        this.faceInfo = faceInfo;
        this.faceLandmark = faceLandmark;
        this.faceAttribute = faceAttribute;
        this.faceFeature = faceFeature;
        this.faceBitmap = faceBitmap;
    }
}
