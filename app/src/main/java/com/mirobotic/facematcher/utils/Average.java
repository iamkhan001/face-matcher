/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.utils;

import java.util.concurrent.atomic.AtomicLong;

public final class Average {
    private final AtomicLong sum = new AtomicLong(0);
    private final AtomicLong size = new AtomicLong(0);

    public void reset() {
        sum.set(0);
        size.set(0);
    }

    public void add(long value) {
        sum.addAndGet(value);
        size.incrementAndGet();
    }

    public double get() {
        return size.get() == 0 ? 0 : sum.doubleValue() / size.get();
    }

    public long size() {
        return size.get();
    }
}
