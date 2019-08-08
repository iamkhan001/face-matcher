/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.validate;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

final class TaskFactory {
    private static final String TAG = "FaceMeSdkTaskFactory";

    @NonNull
    static List<BaseTask> create(File file, @NonNull Observer observer) {
        observer.onParseBegin();

        ArrayList<BaseTask> tasks = new ArrayList<>();
        try {
            String content;
            int lineNum = 1;

            File rootDir = file.getParentFile();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                while ((content = reader.readLine()) != null) {
                    BaseTask task = createTask(observer, rootDir, lineNum, content);
                    if (task != null) tasks.add(task);

                    lineNum++;
                }
            }
            observer.onParseComplete(tasks.size());
        } catch (IOException e) {
            String msg = "Cannot parse file: " + e.getMessage();
            Log.e(TAG, msg, e);
            observer.onParseFailed(msg);
        }

        return tasks;
    }

    private static BaseTask createTask(Observer observer, File rootDir, int lineNum, String content) {
        if (TextUtils.isEmpty(content)) return null;
        if (!content.contains(":")) {
            observer.onWarning(lineNum, "?", " > unexpected content appears: " + content);
            return null;
        }

        String[] args = content.split(":", 2);
        observer.onInit(lineNum, args[0]);

        try {
            switch (args[0]) {
                case "Age":
                    return new AgeTask(observer, rootDir, lineNum, content);
                case "Gender":
                    return new GenderTask(observer, rootDir, lineNum, content);
                case "Emotion":
                    return new EmotionTask(observer, rootDir, lineNum, content);
                case "FaceRecognition":
                    return new FaceRecognitionTask(observer, rootDir, lineNum, content);
                default:
                    return new UnsupportedTask(observer, rootDir, lineNum, content);
            }
        } catch (Exception e) {
            String msg = "Create task failed: " + e.getMessage();
            Log.e(TAG, msg, e);
            observer.onWarning(lineNum, args[0], msg);
            return null;
        }
    }
}
