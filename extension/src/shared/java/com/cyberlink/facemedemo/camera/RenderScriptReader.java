/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.util.Log;
import android.view.Surface;

/**
 * TODO: Under implementing RenderScript YUV to RGBA.
 *
 * @see <a href="http://werner-dittmann.blogspot.com/2016/03/using-android-renderscript-to-convert.html">
 *     Using Android RenderScript to convert YUV image format to RGB format</a>
 */
class RenderScriptReader {
    private static final String TAG = "CamCtrl.RS";

    private final Context context;
    private final RenderScript rs;
    private final RgbConversion rgbConversion;

    RenderScriptReader(Context context, int previewWidth, int previewHeight) {
        this.context = context;
        this.rs = RenderScript.create(context);
        this.rgbConversion = new RgbConversion(rs, previewWidth, previewHeight, 30);
    }

    Surface getSurface() {
        return rgbConversion.getInputSurface();
    }

    void setOutputSurface(Surface surface) {
        rgbConversion.setOutputSurface(surface);
    }

    private static class RgbConversion implements Allocation.OnBufferAvailableListener {
        private Allocation mInputAllocation;
        private Allocation mOutputAllocation;
        private Allocation mOutputAllocationInt;
        private Allocation mScriptAllocation;

        private int[] mOutBufferInt;
        private long mLastProcessed;
        private final int mFrameEveryMs;

        RgbConversion(RenderScript rs, int width, int height, int frameMs) {
            this.mFrameEveryMs = frameMs;

            createAllocations(rs, width, height);

            mInputAllocation.setOnBufferAvailableListener(this);

            //mScriptC = new ScriptC_yuv2rgb(rs);
            //mScriptC.set_gCurrentFrame(mInputAllocation);
            //mScriptC.set_gIntFrame(mOutputAllocationInt);
        }

        private void createAllocations(RenderScript rs, int width, int height) {
            mOutBufferInt = new int[width * height];

            Type.Builder yuvTypeBuilder = new Type.Builder(rs, Element.YUV(rs))
                    .setX(width)
                    .setY(height)
                    .setYuvFormat(ImageFormat.YUV_420_888);
            mInputAllocation = Allocation.createTyped(rs, yuvTypeBuilder.create(),
                    Allocation.USAGE_IO_INPUT | Allocation.USAGE_SCRIPT);

            Type rgbType = Type.createXY(rs, Element.RGBA_8888(rs), width, height);
            Type intType = Type.createXY(rs, Element.U32(rs), width, height);

            mScriptAllocation = Allocation.createTyped(rs, rgbType,
                    Allocation.USAGE_SCRIPT);
            mOutputAllocation = Allocation.createTyped(rs, rgbType,
                    Allocation.USAGE_IO_OUTPUT | Allocation.USAGE_SCRIPT);
            mOutputAllocationInt = Allocation.createTyped(rs, intType,
                    Allocation.USAGE_SCRIPT);
        }

        Surface getInputSurface() {
            return mInputAllocation.getSurface();
        }

        void setOutputSurface(Surface surface) {
            mOutputAllocation.setSurface(surface);
        }

        @Override
        public void onBufferAvailable(Allocation a) {
            Log.i(TAG, "onBufferAvailable");
            mInputAllocation.ioReceive();

            long currentTimeMillis = System.currentTimeMillis();
            if ((currentTimeMillis - mLastProcessed) < mFrameEveryMs) return;

//            //mScriptC.forEach_yuv2rgbFrames(mScriptAllocation, mOutputAllocation);
//            mOutputAllocationInt.copyTo(mOutBufferInt);
//            //mFrameCallback.onFrameArrayInt(mOutBufferInt);
//
//            mLastProcessed = currentTimeMillis;
        }
    }
}
