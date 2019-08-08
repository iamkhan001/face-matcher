/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.validate;

import java.io.File;

class UnsupportedTask extends BaseTask {

    UnsupportedTask(Callback callback, File rootDir, int lineNum, String content) {
        super(callback, rootDir, lineNum, content);
    }

    @Override
    int getArgCount() {
        return 2;
    }

    @Override
    boolean parseArgs() {
        logW("unsupported validation task");
        return false;
    }

    @Override
    boolean doPrepare() {
        return false;
    }

    @Override
    String validate() {
        return "unsupported validation task";
    }
}
