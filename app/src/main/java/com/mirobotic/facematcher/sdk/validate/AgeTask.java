/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.validate;

import com.cyberlink.faceme.FaceAttribute;

import java.io.File;

class AgeTask extends BaseTask {

    private static final int TOLERANCE_RANGE = 10;

    private float answerAge;
    private FaceAttribute faceAttribute;

    AgeTask(Callback callback, File rootDir, int lineNum, String content) {
        super(callback, rootDir, lineNum, content);
    }

    @Override
    int getArgCount() {
        return 3;
    }

    @Override
    boolean parseArgs() {
        answerAge = Float.parseFloat(args[2].trim());
        File image = composeFile(args[1].trim());

        return image != null;
    }

    @Override
    boolean doPrepare() {
        faceAttribute = getFaceAttribute(args[1].trim());

        return faceAttribute != null;
    }

    @Override
    String validate() {
        boolean OK = Math.abs(faceAttribute.age - answerAge) <= TOLERANCE_RANGE;
        if (OK) return null;

        return String.valueOf(faceAttribute.age);
    }
}
