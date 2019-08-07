/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.manageface;

import android.graphics.Bitmap;
import android.util.LruCache;

class FaceBitmapCache {
    private static FaceBitmapCache sFaceBitmapCache;
    private LruCache<Long, Bitmap> memoryCache;

    private FaceBitmapCache() {
        initBitmapMemoryCache();
    }

    public static FaceBitmapCache getInstance() {
        if (sFaceBitmapCache == null) {
            synchronized (FaceBitmapCache.class) {
                if (sFaceBitmapCache == null) {
                    sFaceBitmapCache = new FaceBitmapCache();
                }
            }
        }
        return sFaceBitmapCache;
    }

    private void initBitmapMemoryCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<Long, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(Long key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public void addBitmapToMemoryCache(long key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(long key) {
        return memoryCache.get(key);
    }

    public void cleanCache()
    {
        memoryCache.evictAll();
    }
}
