/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.validate;

import com.cyberlink.faceme.FaceFeature;

import java.io.File;

class FaceRecognitionTask extends BaseTask {

    private boolean answerIdentical;
    private FaceFeature faceA, faceB;

    FaceRecognitionTask(Callback callback, File rootDir, int lineNum, String content) {
        super(callback, rootDir, lineNum, content);
    }

    @Override
    int getArgCount() {
        return 3;
    }

    @Override
    boolean parseArgs() {
        answerIdentical = "1".equals(args[2].trim());

        String[] files = args[1].split(",");
        String imagePathA = files[0].trim();
        String imagePathB = files[1].trim();

        File imageA = composeFile(imagePathA);
        File imageB = composeFile(imagePathB);

        return imageA != null && imageB != null;
    }

    @Override
    boolean doPrepare() {
        String[] files = args[1].split(",");
        String imagePathA = files[0].trim();
        String imagePathB = files[1].trim();

        faceA = getFaceFeature(imagePathA);
        faceB = getFaceFeature(imagePathB);

        return faceA != null && faceB != null;
    }

    @Override
    String validate() {
        float confidence = compare(faceA, faceB);
        float threshold = getPrecisionThreshold();

        boolean questionIdentical = confidence >= threshold;
        boolean OK = questionIdentical == answerIdentical;
        if (OK) return null;

        return questionIdentical ? "1" : "0";
    }
}
