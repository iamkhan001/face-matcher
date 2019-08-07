/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.sdk;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * A demo purpose class.
 *
 * It would be invoked right after {@link FaceMeRecognizerWrapper#extractFace(Bitmap)}
 */
public interface OnExtractedListener {
    /**
     * Callback when faces extracted.
     */
    default void onExtracted(int width, int height, @NonNull List<FaceHolder> faces) {}
}
