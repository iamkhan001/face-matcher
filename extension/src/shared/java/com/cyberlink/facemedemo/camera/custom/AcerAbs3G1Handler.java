/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.camera.custom;

public class AcerAbs3G1Handler implements CustomHandler {
    @Override
    public Integer forceBitmapRotation() {
        return 90;
    }
}
