/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.validate;

import android.text.TextUtils;

import com.cyberlink.faceme.Emotion;
import com.cyberlink.faceme.FaceAttribute;

import java.io.File;

class EmotionTask extends BaseTask {

    private String answerEmotion;
    private FaceAttribute faceAttribute;

    EmotionTask(Callback callback, File rootDir, int lineNum, String content) {
        super(callback, rootDir, lineNum, content);
    }

    @Override
    int getArgCount() {
        return 3;
    }

    @Override
    boolean parseArgs() {
        answerEmotion = args[2].trim();
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
        String questionEmotion = convert(faceAttribute.emotion);
        boolean OK = TextUtils.equals(answerEmotion, questionEmotion);
        if (OK) return null;

        return questionEmotion;
    }

    private static String convert(@Emotion.EEmotion int emotion) {
        switch (emotion) {
            case Emotion.NEUTRAL:
                return "NEUTRAL";
            case Emotion.ANGRY:
                return "ANGRY";
            case Emotion.SAD:
                return "SAD";
            case Emotion.SURPRISED:
                return "SURPRISED";
            case Emotion.HAPPY:
                return "HAPPY";
            case Emotion.UNKNOWN:
                return "UNKNOWN";
            default:
                return "UNKNOWN";
        }
    }
}
