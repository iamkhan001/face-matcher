/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.camerastream;

import android.app.Service;
import android.content.Context;
import android.graphics.Rect;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.cyberlink.faceme.UserAction;
import com.cyberlink.faceme.UserActionDetector;
import com.cyberlink.facemedemo.sdk.FaceHolder;
import com.cyberlink.facemedemo.ui.CLToast;
import com.cyberlink.facemedemo.ui.LicenseInfoHandler;
import com.mirobotic.facematcher.R;
import com.mirobotic.facematcher.view.MaskView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

final class UserInteractionHandler {
    private static final String TAG = "UserInteractionHandler";

    private static final int TOTAL_PASS_COUNT = 4;

    private static final int CENTER_PERIOD_IN_MS = 10000;
    private static final int ONE_ACTION_PERIOD_IN_MS = 5000;
    private static final int ANSWER_FREEZE_PERIOD_IN_MS = 2000;
    private static final int GAP_BETWEEN_TWO_ACTIONS_IN_MS = 500;
    private static final int FINISH_COUNT_DOWN_IN_MS = 3000;

    private final Context context;
    private final View container;
    private final MaskView maskView;
    private final Runnable onDismissCallback;

    private final Handler detectionHandler;
    private final Handler mainHandler;
    private final TextView txtIndicator;
    private final TextView txtMessage;
    private final Vibrator vibrator;

    private final AtomicBoolean isReleased = new AtomicBoolean(false);
    private final AtomicBoolean frozenTime = new AtomicBoolean(true);

    private int numberOfPass = 0;
    @UserAction.EUserAction
    private int currentAction = UserAction.NONE;

    private CountDownTimer countDownTimer = null;
    private UserActionDetector detector = null;

    UserInteractionHandler(Context context, View container, MaskView maskView, Runnable onDismissCallback) {
        Log.d(TAG, "constructor");
        this.context = context.getApplicationContext();

        this.container = container;
        this.maskView = maskView;
        this.onDismissCallback = onDismissCallback;

        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.detectionHandler = new Handler(thread.getLooper());
        this.mainHandler = new Handler(Looper.getMainLooper());

        this.txtIndicator = container.findViewById(R.id.txtUserActionIndicator);
        this.txtMessage = container.findViewById(R.id.txtUserActionMessage);
        this.vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);

        initialize();
    }

    private void initialize() {
        detectionHandler.post(() -> {
            initDetector();

            mainHandler.post(() -> {
                adjustBottomMargin();
                chooseRandomAction();
            });
        });
    }

    private void initDetector() {
        if (isReleased.get()) return;
        try {
            releaseDetector();

            byte[] licenseInfo = LicenseInfoHandler.getLicenseInfo();
            boolean success;
            long start = System.currentTimeMillis();

            detector = new UserActionDetector(context);
            success = detector.initialize(licenseInfo);
            if (!success) throw new IllegalStateException("Initialize user action detector failed");

            Log.v(TAG, " > initDetector took " + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            Log.e(TAG, "Cannot setup FaceMe UserActionDetector", e);
            CLToast.showLong(context, "Cannot setup FaceMe UserActionDetector\n" + e.getMessage());
            releaseDetector();
        }
    }

    private void releaseDetector() {
        if (detector != null) {
            Log.v(TAG, " > releaseDetector");
            detector.release();
            detector = null;
        }
    }

    void rotate() {
        adjustBottomMargin();
    }

    void adjustBottomMargin() {
        int marginBottom = context.getResources().getDimensionPixelSize(R.dimen.uad_component_margin_bottom);
        ((ViewGroup.MarginLayoutParams) container.getLayoutParams()).bottomMargin = marginBottom;
        container.requestLayout();
    }

    void release() {
        cancelTimer();
        if (isReleased.getAndSet(true)) return;

        Log.v(TAG, "release");
        container.setVisibility(View.GONE);
        maskView.setVisibility(View.GONE);
        vibrator.cancel();

        detectionHandler.removeCallbacksAndMessages(null);
        detectionHandler.post(() -> {
            releaseDetector();
            detectionHandler.getLooper().quitSafely();
        });

        mainHandler.removeCallbacksAndMessages(null);
    }

    void detect(float confidenceThreshold, int width, int height, @NonNull List<FaceHolder> faces) {
        if (isReleased.get()) return;
        mainHandler.post(() -> maskView.reset(width, height));

        Rect maskRect = maskView.getBitmapMaskRect();
        ArrayList<FaceHolder> candidates = new ArrayList<>();
        for (FaceHolder face : faces) {
            if (overlap(maskRect, face.faceInfo.boundingBox)) {
                candidates.add(face);
            }
        }

        if (candidates.isEmpty()) return;
        if (candidates.size() > 1) {
            CLToast.show(context, "Cannot interact with " + faces.size() + " faces");
            return;
        }

        FaceHolder faceHolder = candidates.get(0);
        detectionHandler.post(() -> {
            if (isReleased.get() || frozenTime.get()) return;

            boolean isMatched = detector.detect(confidenceThreshold, faceHolder.faceInfo, faceHolder.faceAttribute,
                    faceHolder.faceFeature, maskRect);
            if (isMatched) {
                Log.i(TAG, "Match!");
                frozenTime.set(true);
                mainHandler.post(this::showPass);
            }
        });
    }

    private static boolean overlap(Rect a, Rect b) {
        return a.contains(b.left, b.top) ||
                a.contains(b.right, b.top) ||
                a.contains(b.right, b.bottom) ||
                a.contains(b.left, b.bottom);
    }

    private void setTextNormalState() {
        txtMessage.setEnabled(true);
        txtMessage.setSelected(false);
        txtIndicator.setEnabled(true);
        txtIndicator.setSelected(false);
    }

    private void setTextOnePassState() {
        txtMessage.setEnabled(false);
        txtMessage.setSelected(true);
        txtIndicator.setText("✔");
        txtIndicator.setEnabled(false);
        txtIndicator.setSelected(true);
    }

    private void setTextAllPassState() {
        txtMessage.setText("Verified Pass");
        txtMessage.setEnabled(false);
        txtMessage.setSelected(false);
        txtIndicator.setText("✔");
        txtIndicator.setEnabled(false);
        txtIndicator.setSelected(false);
    }

    private void setTextFailState() {
        txtMessage.setEnabled(true);
        txtMessage.setSelected(true);
        txtIndicator.setText("✖");
        txtIndicator.setEnabled(true);
        txtIndicator.setSelected(true);
    }

    private void chooseRandomAction() {
        if (isReleased.get()) return;
        Log.d(TAG, "chooseRandomAction");
        if (UserAction.NONE == currentAction) {
            currentAction = UserAction.CENTER;
        } else {
            int[] candidates = {
                    UserAction.TURN_HEAD, UserAction.NOD_HEAD, UserAction.SMILE
            };
            Random random = new Random(System.currentTimeMillis());
            do {
                int index = random.nextInt(candidates.length);
                int userAction = candidates[index];
                if (userAction != currentAction) {
                    currentAction = userAction;
                    break;
                }
            } while (true);
        }

        detectionHandler.post(() -> detector.setUserAction(currentAction));

        String message;
        if (UserAction.CENTER == currentAction) {
            Log.v(TAG, " > CENTER");
            message = "Put your pose in center of camera";
        } else if (UserAction.TURN_HEAD == currentAction) {
            Log.v(TAG, " > TURN_HEAD");
            message = "Turn your head to the left or right";
        } else if (UserAction.NOD_HEAD == currentAction) {
            Log.v(TAG, " > NOD_HEAD");
            message = "Nod your head";
        } else if (UserAction.SMILE == currentAction) {
            Log.v(TAG, " > SMILE");
            message = "Please smile";
        } else {
            Log.w(TAG, " > unknown: " + currentAction);
            message = "Unexpected situation";
        }

        container.setVisibility(View.VISIBLE);
        maskView.setVisibility(View.VISIBLE);

        txtMessage.setText(message);
        txtIndicator.setText(String.valueOf(getActionPeriod() / 1000));
        setTextNormalState();

        detectionHandler.post(() -> frozenTime.set(false));
        startDetectActionPeriodTimer();
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private long getActionPeriod() {
        return UserAction.CENTER == currentAction ? CENTER_PERIOD_IN_MS : ONE_ACTION_PERIOD_IN_MS;
    }

    private void startDetectActionPeriodTimer() {
        Log.v(TAG, " > startDetectActionPeriodTimer");
        cancelTimer();
        countDownTimer = new CountDownTimer(getActionPeriod(), 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                txtIndicator.setText(String.valueOf((int) Math.ceil(millisUntilFinished / 1000F)));
            }

            @Override
            public void onFinish() {
                Log.w(TAG, "onFinish (timed out)");
                long[] pattern = {0, 100, 100, 100, 100, 100}; // 3 short.
                vibrator.vibrate(pattern, -1);
                setTextFailState();
                delayDismiss();
            }
        }.start();
    }

    private void showPass() {
        Log.d(TAG, "showPass: " + (numberOfPass+1));
        cancelTimer();
        if (isReleased.get()) return;
        numberOfPass++;

        if (numberOfPass >= TOTAL_PASS_COUNT) {
            vibrator.vibrate(250); // 1 long.
            setTextAllPassState();
            delayDismiss();
        } else {
            vibrator.vibrate(100); // 1 short.
            setTextOnePassState();
            mainHandler.postDelayed(this::nextAction, ANSWER_FREEZE_PERIOD_IN_MS);
        }
    }

    private void nextAction() {
        container.animate().alpha(0).setDuration(GAP_BETWEEN_TWO_ACTIONS_IN_MS / 2)
                .withEndAction(() -> {
                    container.setVisibility(View.GONE);
                    container.animate().alpha(1).setDuration(GAP_BETWEEN_TWO_ACTIONS_IN_MS / 2)
                            .withEndAction(this::chooseRandomAction)
                            .start();
                }).start();
    }

    private void delayDismiss() {
        Log.d(TAG, " > delayDismiss");
        frozenTime.set(true);

        mainHandler.postDelayed(() -> {
            onDismissCallback.run();
            release();
        }, FINISH_COUNT_DOWN_IN_MS);
    }
}
