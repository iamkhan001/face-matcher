/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.manageface;

import com.cyberlink.facemedemo.sdk.FaceData;

interface IManageFaceListener {
    void onDeleteFace(FaceData faceData);
}
