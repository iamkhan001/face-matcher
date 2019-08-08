/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

public class MaskView extends View {
    private static final String TAG = "MaskView";

    private final Rect layoutMaskRect = new Rect();
    private final Rect bitmapMaskRect = new Rect();

    private final Paint maskPaint;

    private int viewWidth;
    private int viewHeight;
    private int bitmapWidth;
    private int bitmapHeight;
    private float bitmapAspectRatio;

    public MaskView(Context context) {
        this(context, null);
    }

    public MaskView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaskView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        maskPaint = new Paint();
        maskPaint.setColor(Color.argb(0x88, 0x00, 0x00, 0x00));
        maskPaint.setStyle(Paint.Style.FILL);
    }

    public Rect getBitmapMaskRect() {
        return bitmapMaskRect;
    }

    public void reset(int width, int height) {
        if (bitmapWidth == width && bitmapHeight == height) return;

        bitmapWidth = width;
        bitmapHeight = height;
        bitmapAspectRatio = 1F * width / height;

        Log.d(TAG, "Bitmap resize: " + width + "x" + height);
        adjustBitmapMask();

        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (left >= right || top >= bottom) return;

        adjustViewMask(right - left, bottom - top);
    }

    private void adjustViewMask(int width, int height) {
        if (viewWidth == width && viewHeight == height) return;

        Log.d(TAG, "View resize: " + width + "x" + height);
        viewWidth = width;
        viewHeight = height;

        float maskHeight = ((width > height) ? height : width) * 0.75F;
        float maskWidth = maskHeight * 0.85F;

        int centerX = width / 2;
        int centerY = height / 2;

        layoutMaskRect.set(
                centerX - (int)(maskWidth / 2),
                centerY - (int)(maskHeight / 2),
                centerX + (int)(maskWidth / 2),
                centerY + (int)(maskHeight / 2)
        );

        // If screen is in portrait mode, move mask axis higher to force user to put their face angle
        // to be more straight to camera.
        if (height > width) {
            int offset = layoutMaskRect.width() / 4;
            layoutMaskRect.top = Math.max(layoutMaskRect.top - offset, 0);
            layoutMaskRect.bottom = Math.max(layoutMaskRect.bottom - offset, 0);
        }
        Log.v(TAG, " > layoutMask: " + layoutMaskRect);

        adjustBitmapMask();
    }

    private void adjustBitmapMask() {
        bitmapMaskRect.setEmpty();

        if (layoutMaskRect.isEmpty()) return;
        if (bitmapWidth == 0 || bitmapHeight == 0) return;

        float viewAspectRatio = 1F * viewWidth / viewHeight;
        // Ignore when aspect ratio is different. Wait for next round to trigger this function.
        if (Math.abs(bitmapAspectRatio - viewAspectRatio) > 0.2) return;

        float relativeWidthRatio = 1F * bitmapWidth / viewWidth;
        float relativeHeightRatio = 1F * bitmapHeight / viewHeight;

        bitmapMaskRect.set(
                (int)(layoutMaskRect.left * relativeWidthRatio),
                (int)(layoutMaskRect.top * relativeHeightRatio),
                (int)(layoutMaskRect.right * relativeWidthRatio),
                (int)(layoutMaskRect.bottom * relativeHeightRatio)
        );
        Log.v(TAG, " > bitmapMask: " + bitmapMaskRect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (layoutMaskRect.isEmpty()) return;

        // 1, 2, 3
        canvas.drawRect(0, 0, viewWidth, layoutMaskRect.top, maskPaint);
        // 4
        canvas.drawRect(0, layoutMaskRect.top, layoutMaskRect.left, layoutMaskRect.bottom, maskPaint);
        // 6
        canvas.drawRect(layoutMaskRect.right, layoutMaskRect.top, viewWidth, layoutMaskRect.bottom, maskPaint);
        // 7, 8, 9
        canvas.drawRect(0, layoutMaskRect.bottom, viewWidth, viewHeight, maskPaint);
    }
}
