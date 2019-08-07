/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.validate;

import android.text.TextUtils;

import com.cyberlink.faceme.FaceAttribute;
import com.cyberlink.faceme.Gender;

import java.io.File;

class GenderTask extends BaseTask {

    private String answerGender;
    private FaceAttribute faceAttribute;

    GenderTask(Callback callback, File rootDir, int lineNum, String content) {
        super(callback, rootDir, lineNum, content);
    }

    @Override
    int getArgCount() {
        return 3;
    }

    @Override
    boolean parseArgs() {
        answerGender = args[2].trim();
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
        String questionGender = convert(faceAttribute.gender);
        boolean OK = TextUtils.equals(answerGender, questionGender);
        if (OK) return null;

        return questionGender;
    }

    private static String convert(@Gender.EGender int gender) {
        switch (gender) {
            case Gender.FEMALE:
                return "FEMALE";
            case Gender.MALE:
                return "MALE";
            case Gender.UNKNOWN:
                return "UNKNOWN";
            default:
                return "UNKNOWN";
        }
    }
}
