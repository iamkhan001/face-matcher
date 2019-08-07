/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.StringRes;

/**
 * A straightforward utility class to show a {@link Toast}.
 * <p/>
 * <b>NOTE</b>:
 * <ul>
 *     <li>Previous {@link Toast} shown via this class will be cancelled.</li>
 *     <li>It doesn't determine if UI is visible. Be careful to consider if it's suitable
 *         to show Toast after callback, such as app is in background, previous Activity
 *         has been destroyed, etc.</li>
 * </ul>
 */
public class CLToast {
    private static final String TAG = "CLToast";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static Toast toast = null;

    private static void showInternal(Context context, String msg, int duration) {
        mainHandler.removeCallbacksAndMessages(null); // Clear and cancel all pending messages.
        mainHandler.post(() -> {
            if (toast != null) {
                toast.cancel();
            }

            Log.v(TAG, msg);
            toast = Toast.makeText(context, msg, duration);
            toast.show();
        });
    }

    public static void show(Context context, @StringRes int msgResId, Object... args) {
        showShort(context, msgResId, args);
    }

    public static void show(Context context, String msg) {
        showShort(context, msg);
    }

    public static void showShort(Context context, @StringRes int msgResId, Object... args) {
        showShort(context, context.getString(msgResId, args));
    }

    /**
     * Show a Toast with short duration explicitly.
     */
    public static void showShort(Context context, String msg) {
        showInternal(context, msg, Toast.LENGTH_SHORT);
    }

    public static void showLong(Context context, @StringRes int msgResId) {
        showLong(context, context.getString(msgResId));
    }

    /**
     * Show a Toast with long duration explicitly.
     */
    public static void showLong(Context context, String msg) {
        showInternal(context, msg, Toast.LENGTH_LONG);
    }
}
