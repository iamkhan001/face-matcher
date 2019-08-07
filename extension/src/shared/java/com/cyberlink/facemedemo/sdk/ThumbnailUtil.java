/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.sdk;

import android.graphics.Rect;
import android.util.Size;

class ThumbnailUtil {

    static Rect enlargeThumbnail(Rect src, Size image_size, float enlargeRatio, boolean isShift) {
        int h_enlarge_pixel = (int) (src.width() * enlargeRatio);
        int v_enlarge_pixel = (int) (src.height() * enlargeRatio);
        return enlargeThumbnail(src, image_size, h_enlarge_pixel, v_enlarge_pixel, h_enlarge_pixel, v_enlarge_pixel, isShift);
    }

    static Rect enlargeThumbnail(Rect src, Size imageSize, int leftSpace, int topSpace, int rightSpace, int bottomSpace, boolean isShift) {
        Rect dst_roi = new Rect();
        // for horizontal enlarge
        dst_roi.left = Math.max(0, src.left - leftSpace);
        if (isShift) {
            dst_roi.right = dst_roi.left + src.width() + leftSpace + rightSpace;
            if ((dst_roi.right) > imageSize.getWidth()) {
                dst_roi.left = imageSize.getWidth() - dst_roi.width();
                if (dst_roi.left < 0) {
                    dst_roi.left = 0;
                    dst_roi.right = imageSize.getWidth();
                }
            }
        } else {
            int right = Math.min(imageSize.getWidth(), src.left + src.width() + rightSpace);
            dst_roi.right = right;
        }

        // for vertical enlarge
        dst_roi.top = Math.max(0, src.top - topSpace);
        if (isShift) {
            dst_roi.bottom = dst_roi.top + src.height() + topSpace + bottomSpace;
            if ((dst_roi.bottom) > imageSize.getHeight()) {
                dst_roi.top = imageSize.getHeight() - dst_roi.height();
                if (dst_roi.top < 0) {
                    dst_roi.top = 0;
                    dst_roi.bottom = imageSize.getHeight();
                }
            }
        } else {
            int bottom = Math.min(imageSize.getHeight(), src.top + src.height() + bottomSpace);
            dst_roi.bottom = bottom;
        }

        return dst_roi;
    }
}