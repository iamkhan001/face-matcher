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
import android.graphics.Point;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.content.ContextCompat;

import com.cyberlink.faceme.Emotion;
import com.cyberlink.faceme.FaceAttribute;
import com.cyberlink.faceme.FaceFeature;
import com.cyberlink.faceme.FaceInfo;
import com.cyberlink.faceme.FaceLandmark;
import com.cyberlink.faceme.Gender;
import com.cyberlink.faceme.Pose;
import com.cyberlink.facemedemo.sdk.FaceData;
import com.cyberlink.facemedemo.sdk.FaceHolder;
import com.cyberlink.facemedemo.ui.UiSettings;
import com.mirobotic.facematcher.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FaceView extends View {
    private static final String TAG = "FaceView";

    public interface OnFaceClickListener {
        void onFaceClick(FaceHolder faceHolder);
    }

    private final int BOUNDING_WIDTH;
    private final int DEFAULT_FONT_SIZE;
    private final int MIN_FONT_SIZE;
    private final int PADDING;
    private final int LANDMARK_WIDTH;
    private final int LANDMARK_RADIUS;

    private final Pair<Integer, Integer> nonameBorderColor, namedBorderColor,
            autoNamedBorderColor, maleBorderColor, femaleBorderColor;

    private final GestureDetector gestureDetector;
    private final List<FaceHolder> faceHolders = new ArrayList<>();

    private final Paint boundingBoxPaint;
    private final Paint landmarkPaint;
    private final Paint textBgPaint;
    private final TextPaint textPaint;

    private UiSettings uiSettings;
    private OnFaceClickListener onFaceClickListener;

    private int bitmapWidth;
    private int bitmapHeight;
    private float bitmapAspectRatio;

    private float relativeWidthRatio;
    private float relativeHeightRatio;

    public FaceView(Context context) {
        this(context, null);
    }

    public FaceView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                determineFaceAndCallback(e.getX(), e.getY());
                return true;
            }
        });

        BOUNDING_WIDTH = context.getResources().getDimensionPixelSize(R.dimen.face_border_width);
        DEFAULT_FONT_SIZE = context.getResources().getDimensionPixelSize(R.dimen.face_attr_default_text_size);
        MIN_FONT_SIZE = context.getResources().getDimensionPixelSize(R.dimen.face_attr_min_text_size);
        PADDING = context.getResources().getDimensionPixelSize(R.dimen.face_attr_padding);
        LANDMARK_WIDTH = context.getResources().getDimensionPixelSize(R.dimen.landmark_width);
        LANDMARK_RADIUS = context.getResources().getDimensionPixelSize(R.dimen.landmark_radius);

        nonameBorderColor = Pair.create(
                ContextCompat.getColor(context, R.color.noname_border),
                ContextCompat.getColor(context, R.color.noname_corner_border)
        );
        namedBorderColor = Pair.create(
                ContextCompat.getColor(context, R.color.named_border),
                ContextCompat.getColor(context, R.color.named_corner_border)
        );
        autoNamedBorderColor = Pair.create(
                ContextCompat.getColor(context, R.color.auto_named_border),
                ContextCompat.getColor(context, R.color.auto_named_corner_border)
        );
        maleBorderColor = Pair.create(
                ContextCompat.getColor(context, R.color.male_border),
                ContextCompat.getColor(context, R.color.male_corner_border)
        );
        femaleBorderColor = Pair.create(
                ContextCompat.getColor(context, R.color.female_border),
                ContextCompat.getColor(context, R.color.female_corner_border)
        );

        boundingBoxPaint = new Paint();
        boundingBoxPaint.setStrokeWidth(BOUNDING_WIDTH);
        boundingBoxPaint.setStyle(Paint.Style.STROKE);

        landmarkPaint = new Paint();
        landmarkPaint.setColor(ContextCompat.getColor(context, R.color.landmark_border));
        landmarkPaint.setStrokeWidth(LANDMARK_WIDTH);
        landmarkPaint.setStyle(Paint.Style.STROKE);

        textBgPaint = new Paint();
        textBgPaint.setColor(ContextCompat.getColor(context, R.color.face_label_bg));
        textBgPaint.setStyle(Paint.Style.FILL);

        textPaint = new TextPaint();
        textPaint.setColor(Color.WHITE);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(DEFAULT_FONT_SIZE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setLinearText(true);
    }

    public void setUiSettings(UiSettings uiSettings) {
        this.uiSettings = uiSettings;
    }

    public void setOnFaceClickListener(OnFaceClickListener listener) {
        this.onFaceClickListener = listener;
    }

    private void determineFaceAndCallback(float x, float y) {
        if (onFaceClickListener == null) return;

        int remapX = (int) (x / relativeWidthRatio);
        int remapY = (int) (y / relativeHeightRatio);

        FaceHolder faceHolder = null;
        for (FaceHolder holder : faceHolders) {
            if (holder.faceInfo.boundingBox.contains(remapX, remapY)) {
                faceHolder = holder;
                break;
            }
        }

        onFaceClickListener.onFaceClick(faceHolder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);

        return true;
    }

    @UiThread
    public void updateFaceAttributes(int width, int height, List<FaceHolder> holders) {
        reset(width, height);

        if (holders != null) this.faceHolders.addAll(holders);

        invalidate();
    }

    private void reset(int width, int height) {
        bitmapWidth = width;
        bitmapHeight = height;
        bitmapAspectRatio = 1F * width / height;

        faceHolders.clear();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long tsDraw = System.currentTimeMillis();
        super.onDraw(canvas);

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        float canvasAspectRatio = 1F * viewWidth / viewHeight;
        // Ignore when aspect ratio is different. Wait for feed with new Face.
        if (Math.abs(bitmapAspectRatio - canvasAspectRatio) > 0.2) return;

        relativeWidthRatio = 1F * viewWidth / bitmapWidth;
        relativeHeightRatio = 1F * viewHeight / bitmapHeight;

        drawFaces(canvas);

        long duration = System.currentTimeMillis() - tsDraw;
        if (duration > 16) {
            Log.w(TAG, "onDraw faces[" + faceHolders.size() + "] took " + (System.currentTimeMillis() - tsDraw) + "ms");
        }
    }

    private void drawFaces(Canvas canvas) {
        for (FaceHolder holder : faceHolders) {
            FaceInfo faceInfo = holder.faceInfo;
            FaceAttribute faceAttr = holder.faceAttribute;
            FaceFeature faceFeature = holder.faceFeature;
            float left = faceInfo.boundingBox.left * relativeWidthRatio;
            float top = faceInfo.boundingBox.top * relativeHeightRatio;
            float right = faceInfo.boundingBox.right * relativeWidthRatio;
            float bottom = faceInfo.boundingBox.bottom * relativeHeightRatio;

            Boolean named = determineName(holder.data);
            Boolean gender = determineGender(faceAttr);

            drawFaceBoundingBox(canvas, named, gender, left, top, right, bottom);
            drawFaceLandmarkIfNeeded(canvas, holder.faceLandmark);

            if (uiSettings.isShowFeatures() && faceAttr != null) {
                // Draw attributes.
                final float availableWidth = (right - left - PADDING * 2 + BOUNDING_WIDTH) * 1.3F;
                final float anchorX = left - BOUNDING_WIDTH / 2;
                float anchorY;

                // Draw face name if named before.
                if (named == null || named) {
                    anchorY = Math.max(top - BOUNDING_WIDTH / 2 - DEFAULT_FONT_SIZE - PADDING * 2, 0);
                    drawText(canvas, anchorX, anchorY, holder.data.name + ", " + format(holder.data.confidence), availableWidth);
                }
                anchorY = bottom + BOUNDING_WIDTH / 2;
                // Draw Age
                if (uiSettings.isShowAge()) {
                    anchorY = drawText(canvas, anchorX, anchorY, "Age: " + reviseAge(faceAttr.age), availableWidth);
                }
                // Draw Gender
                if (uiSettings.isShowGender()) {
                    anchorY = drawText(canvas, anchorX, anchorY, "Gender: " + reviseGender(faceAttr.gender), availableWidth);
                }
                // Draw Emotion
                if (uiSettings.isShowEmotion()) {
                    anchorY = drawText(canvas, anchorX, anchorY, "Emotion: " + reviseEmotion(faceAttr.emotion), availableWidth);
                }
                // Draw Pose
                if (uiSettings.isShowPose()) {
                    anchorY = drawText(canvas, anchorX, anchorY, "Angle: " + revisePose(faceAttr.pose), availableWidth * 1.3F);
                }
            }
        }
    }

    private String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private Boolean determineName(FaceData data) {
        if (TextUtils.isEmpty(data.name))
            return false;
        else if (("User#" + data.collectionId).equals(data.name))
            return null;
        else
            return true;
    }
    private Boolean determineGender(FaceAttribute faceAttribute) {
        if (faceAttribute != null && uiSettings.isShowFeatures() && uiSettings.isShowGender()) {
            if (faceAttribute.gender == Gender.MALE)
                return true;
            else if (faceAttribute.gender == Gender.FEMALE)
                return false;
        }
        return null;
    }

    private void drawFaceBoundingBox(Canvas canvas, Boolean named, Boolean gender, float left, float top, float right, float bottom) {
        Pair<Integer, Integer> borderColor;
        if (named == null) {
            borderColor = autoNamedBorderColor;
        } else if (named) {
            if (gender == null) {
                borderColor = namedBorderColor;
            } else {
                borderColor = gender ? maleBorderColor : femaleBorderColor;
            }
        } else {
            borderColor = nonameBorderColor;
        }

        boundingBoxPaint.setColor(borderColor.first);
        canvas.drawRect(left, top, right, bottom, boundingBoxPaint);

        boundingBoxPaint.setColor(borderColor.second);
        drawFaceBoundingBoxCorner(canvas, boundingBoxPaint, left, top, right, bottom);
    }

    private void drawFaceBoundingBoxCorner(Canvas canvas, Paint cornerPaint, float left, float top, float right, float bottom) {
        float width = right - left;
        float height = bottom - top;

        // top, left, outer
        canvas.drawLine(left - BOUNDING_WIDTH / 2, top - BOUNDING_WIDTH, left + width * 0.333_333_3F, top - BOUNDING_WIDTH, cornerPaint);
        // top, left, inner
        canvas.drawLine(left + BOUNDING_WIDTH / 2, top + BOUNDING_WIDTH, left + width * 0.333_333_3F, top + BOUNDING_WIDTH, cornerPaint);
        // top, right, outer
        canvas.drawLine(left + width * 0.666_666_7F, top - BOUNDING_WIDTH, right + BOUNDING_WIDTH / 2, top - BOUNDING_WIDTH, cornerPaint);
        // top, right, inner
        canvas.drawLine(left + width * 0.666_666_7F, top + BOUNDING_WIDTH, right - BOUNDING_WIDTH / 2, top + BOUNDING_WIDTH, cornerPaint);

        // right, top, outer
        canvas.drawLine(right + BOUNDING_WIDTH, top - BOUNDING_WIDTH * 1.5F, right + BOUNDING_WIDTH, top + height * 0.333_333_3F, cornerPaint);
        // right, top, inner
        canvas.drawLine(right - BOUNDING_WIDTH, top + BOUNDING_WIDTH * 1.5F, right - BOUNDING_WIDTH, top + height * 0.333_333_3F, cornerPaint);
        // right, bottom, outer
        canvas.drawLine(right + BOUNDING_WIDTH, top + height * 0.666_666_7F, right + BOUNDING_WIDTH, bottom + BOUNDING_WIDTH * 1.5F, cornerPaint);
        // right, bottom, outer
        canvas.drawLine(right - BOUNDING_WIDTH, top + height * 0.666_666_7F, right - BOUNDING_WIDTH, bottom - BOUNDING_WIDTH * 1.5F, cornerPaint);

        // bottom, right, outer
        canvas.drawLine(left + width * 0.666_666_7F, bottom + BOUNDING_WIDTH, right + BOUNDING_WIDTH / 2, bottom + BOUNDING_WIDTH, cornerPaint);
        // bottom, right, inner
        canvas.drawLine(left + width * 0.666_666_7F, bottom - BOUNDING_WIDTH, right - BOUNDING_WIDTH / 2, bottom - BOUNDING_WIDTH, cornerPaint);
        // bottom, left, outer
        canvas.drawLine(left - BOUNDING_WIDTH / 2, bottom + BOUNDING_WIDTH, left + width * 0.333_333_3F, bottom + BOUNDING_WIDTH, cornerPaint);
        // bottom, left, inner
        canvas.drawLine(left + BOUNDING_WIDTH / 2, bottom - BOUNDING_WIDTH, left + width * 0.333_333_3F, bottom - BOUNDING_WIDTH, cornerPaint);

        // left, bottom, outer
        canvas.drawLine(left - BOUNDING_WIDTH, top + height * 0.666_666_7F, left - BOUNDING_WIDTH, bottom + BOUNDING_WIDTH * 1.5F, cornerPaint);
        // left, bottom, inner
        canvas.drawLine(left + BOUNDING_WIDTH, top + height * 0.666_666_7F, left + BOUNDING_WIDTH, bottom - BOUNDING_WIDTH * 1.5F, cornerPaint);

        // left, top, outer
        canvas.drawLine(left - BOUNDING_WIDTH, top - BOUNDING_WIDTH * 1.5F, left - BOUNDING_WIDTH, top + height * 0.333_333_3F, cornerPaint);
        // left, top, inner
        canvas.drawLine(left + BOUNDING_WIDTH, top + BOUNDING_WIDTH * 1.5F, left + BOUNDING_WIDTH, top + height * 0.333_333_3F, cornerPaint);
    }

    private void drawFaceLandmarkIfNeeded(Canvas canvas, FaceLandmark landmark) {
        if (!uiSettings.isShowLandmark() || landmark == null) return;
        if (landmark.featurePoints == null || landmark.featurePoints.length == 0) return;

        for (Point featurePoint : landmark.featurePoints) {
            canvas.drawCircle(featurePoint.x * relativeWidthRatio,
                    featurePoint.y * relativeHeightRatio,
                    LANDMARK_RADIUS, landmarkPaint);
        }
    }

    private String reviseAge(float age) {
        if (uiSettings.isAgeInRange()) {
            int base = (int) (age / 5);
            int max = (base + 1) * 5;
            int min = (base) * 5;
            return min + " ~ " + max;
        } else {
            return String.valueOf(age);
        }
    }

    private String reviseGender(@Gender.EGender int gender) {
        switch (gender) {
            case Gender.MALE: return "Male"; // \u2642
            case Gender.FEMALE: return "Female"; // \u2640

            case Gender.UNKNOWN:
            default: return "Unknown";
        }
    }

    private String reviseEmotion(@Emotion.EEmotion int emotion) {
        switch (emotion) {
            case Emotion.HAPPY: return "Happy";
            case Emotion.SURPRISED: return "Surprised";
            case Emotion.SAD: return "Sad";
            case Emotion.ANGRY: return "Angry";
            case Emotion.NEUTRAL: return "Neutral";

            case Emotion.UNKNOWN:
            default: return "Unknown";
        }
    }

    private String revisePose(Pose pose) {
        return "(Y: " + (int)pose.yaw + ", P: " + (int)pose.pitch + ", R: " + (int)pose.roll + ")";
    }

    private float drawText(Canvas canvas, float left, float anchorY, String string, float availableWidth) {
        Rect txtBounds = new Rect();
        int fontSize = DEFAULT_FONT_SIZE;
        do {
            textPaint.setTextSize(fontSize--);
            textPaint.getTextBounds(string, 0, string.length(), txtBounds);
        } while (txtBounds.width() >= availableWidth && fontSize > MIN_FONT_SIZE);

        float bgHeight = txtBounds.height() + PADDING * 2;
        float textY = anchorY + txtBounds.height() + PADDING / 2;
        canvas.drawRect(left, anchorY, left + txtBounds.width() + PADDING * 2, anchorY + bgHeight, textBgPaint);
        canvas.drawText(string, left + PADDING, textY, textPaint);
        anchorY += txtBounds.height() + PADDING * 2;

        return anchorY;
    }
}
