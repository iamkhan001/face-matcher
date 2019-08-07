/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.validate;

import java.io.File;

public interface Observer extends BaseTask.Callback {
    void onParseBegin();
    void onInit(int lineNum, String name);
    void onWarning(int lineNum, String name, String msg);
    void onParseFailed(String msg);
    void onParseComplete(int countOfTasks);

    void onInfo(String msg);
    void onVerbose(String msg);
    void onValidateBegin();
    void onValidateComplete(File outputFile);
}
