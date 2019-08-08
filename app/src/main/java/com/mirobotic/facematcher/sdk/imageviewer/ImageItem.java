/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.imageviewer;

import android.util.Size;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Locale;

class ImageItem {

    final File file;
    private Size resolution = new Size(0, 0);

    boolean selected = false;

    ImageItem(File file) {
        this.file = file;
    }

    void setResolution(@NonNull Size resolution) {
        this.resolution = resolution;
    }

    Size getResolution() {
        return resolution;
    }

    String getFileSize() {
        long bytes = file.length();
        if (bytes < 1024) {
            return String.format(Locale.US, "%dB", bytes);
        } else if (bytes < 1048576) {
            return String.format(Locale.US, "%.1fKB", bytes / 1024F);
        } else {
            return String.format(Locale.US, "%.1fMB", bytes / 1048576F);
        }
    }
}
