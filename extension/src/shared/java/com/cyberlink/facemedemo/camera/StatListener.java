/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.camera;


import androidx.annotation.IntRange;

import java.util.Arrays;

public class StatListener {

    /**
     * A stat class to average numbers of values to smooth average curve.
     */
    private static class MovingAverage {

        private final int size;

        private transient boolean ignore;
        private transient int min;
        private transient double stored;
        private transient double sum;
        private transient final double[] circularBuffer;
        private transient int circularBufferIndex;

        MovingAverage(@IntRange(from=1) int size) {
            this.size = size;
            this.circularBuffer = new double[size];
            this.ignore = true;
        }

        void reset() {
            Arrays.fill(circularBuffer, 0);
            circularBufferIndex = 0;
            stored = 0;
            sum = 0;
            min = 0;
            ignore = true;
        }

        void store(double value) {
            stored += value;
            ignore = false;
        }

        void addValue() {
            if (ignore) return;

            double value = stored;
            stored = 0;

            min = Math.min(size, min + 1);
            sum -= circularBuffer[circularBufferIndex];
            circularBuffer[circularBufferIndex++] = value;
            sum += value;
            if (circularBufferIndex >= size) {
                circularBufferIndex = 0;
            }
        }

        double getAverage() {
            return min == 0 ? 0 : sum / (double) min;
        }
    }

    private static final int ACCUMULATED_NUMBER = 3;

    private final MovingAverage avgFramesCaptured = new MovingAverage(ACCUMULATED_NUMBER);
    private final MovingAverage avgImagesCaptured = new MovingAverage(ACCUMULATED_NUMBER);

    private final MovingAverage avgBitmapsCreated = new MovingAverage(ACCUMULATED_NUMBER);
    private final MovingAverage avgTimeBitmapsRotated = new MovingAverage(ACCUMULATED_NUMBER);
    private final MovingAverage avgTimeBitmapsTook = new MovingAverage(ACCUMULATED_NUMBER);

    private final MovingAverage avgFacesRecognized = new MovingAverage(ACCUMULATED_NUMBER);
    private final MovingAverage avgTimeFacesTotal = new MovingAverage(ACCUMULATED_NUMBER);
    private final MovingAverage avgTimeNormalFaceDetected = new MovingAverage(ACCUMULATED_NUMBER);
    private final MovingAverage avgTimeFastFaceDetected = new MovingAverage(ACCUMULATED_NUMBER);
    private final MovingAverage avgTimeFacesExtracted = new MovingAverage(ACCUMULATED_NUMBER);
    private final MovingAverage avgTimeFacesRecognized = new MovingAverage(ACCUMULATED_NUMBER);

    /** Reset all average values */
    public void reset() {
        avgFramesCaptured.reset();
        avgImagesCaptured.reset();

        avgBitmapsCreated.reset();
        avgTimeBitmapsRotated.reset();
        avgTimeBitmapsTook.reset();

        avgFacesRecognized.reset();
        avgTimeFacesTotal.reset();
        avgTimeNormalFaceDetected.reset();
        avgTimeFastFaceDetected.reset();
        avgTimeFacesExtracted.reset();
        avgTimeFacesRecognized.reset();
    }

    /** Update all average stored value and add into average count */
    public void update() {
        avgFramesCaptured.addValue();
        avgImagesCaptured.addValue();
        avgBitmapsCreated.addValue();
        avgFacesRecognized.addValue();
    }

    void onFrameCaptured() {
        avgFramesCaptured.store(1);
    }

    void onImageCaptured() {
        avgImagesCaptured.store(1);
    }

    void onBitmapRotated(long duration) {
        avgTimeBitmapsRotated.store(duration);
        avgTimeBitmapsRotated.addValue();
    }

    void onBitmapCreated(long duration) {
        avgBitmapsCreated.store(1);
        avgTimeBitmapsTook.store(duration);
        avgTimeBitmapsTook.addValue();
    }

    public void onFacesRecognized(int count, long total, long normalFaceDetect, long fastFaceDetect, long extract, long recognize) {
        avgFacesRecognized.store(count);
        avgTimeFacesTotal.store(total);
        avgTimeFacesTotal.addValue();

        if (normalFaceDetect > 0) {
            avgTimeNormalFaceDetected.store(normalFaceDetect);
            avgTimeNormalFaceDetected.addValue();
        }
        if (fastFaceDetect > 0) {
            avgTimeFastFaceDetected.store(fastFaceDetect);
            avgTimeFastFaceDetected.addValue();
        }
        if (extract > 0) {
            avgTimeFacesExtracted.store(extract);
            avgTimeFacesExtracted.addValue();
        }
        if (count > 0) { // recognize time is too small, so we depend on count instead.
            avgTimeFacesRecognized.store(recognize);
            avgTimeFacesRecognized.addValue();
        }
    }

    public double getAverageFrameCaptured() {
        return avgFramesCaptured.getAverage();
    }

    public double getAverageImageCaptured() {
        return avgImagesCaptured.getAverage();
    }

    public double getAverageBitmapCreated() {
        return avgBitmapsCreated.getAverage();
    }

    public double getAverageBitmapRotatedTime() {
        return avgTimeBitmapsRotated.getAverage();
    }

    public double getAverageBitmapCreatedTime() {
        return avgTimeBitmapsTook.getAverage();
    }

    public double getAverageFaceRecognized() {
        return avgFacesRecognized.getAverage();
    }

    public double getAverageFaceTotalTime() {
        return avgTimeFacesTotal.getAverage();
    }

    public double getAverageNormalFaceDetectedTime() {
        return avgTimeNormalFaceDetected.getAverage();
    }

    public double getAverageFastFaceDetectedTime() {
        return avgTimeFastFaceDetected.getAverage();
    }

    public double getAverageFaceExtractedTime() {
        return avgTimeFacesExtracted.getAverage();
    }

    public double getAverageFaceRecognizedTime() {
        return avgTimeFacesRecognized.getAverage();
    }
}
