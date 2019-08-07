/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.cyberlink.facemedemo.extension.R;

public abstract class BaseDialogFragment<B extends BaseDialogFragment.IBroker> extends DialogFragment {

    protected static final String ARG_FULLSCREEN = "fullscreen";

    public interface IBroker {
        default <F extends BaseDialogFragment> void onFragmentDetach(F fragment) {}
    }

    private boolean isFullscreen = true;

    protected B broker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            isFullscreen = arguments.getBoolean(ARG_FULLSCREEN);
        }

        if (isFullscreen) {
            setStyle(STYLE_NO_TITLE, R.style.CLDialog_Fullscreen);
        } else {
            setStyle(STYLE_NO_TITLE, R.style.CLDialog);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        if (isFullscreen) {
            // Temporarily set the dialogs window to not focusable to prevent the short
            // popup of the navigation bar.
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }

        return dialog;
    }

    public void show(FragmentManager manager) {
        // Show the dialog.
        super.show(manager, getTagId());

        // It is necessary to call executePendingTransactions() on the FragmentManager
        // before hiding the navigation bar, because otherwise getWindow() would raise a
        // NullPointerException since the window was not yet created.
        manager.executePendingTransactions();

        if (isFullscreen) {
            // Make the dialogs window focusable again.
            getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(getLayoutResId(), container);
        initUiComponents(inflater, rootView);
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof IBroker) {
            broker = (B) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        broker.onFragmentDetach(this);
        broker = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isFullscreen) {
            adjustFullscreenMode();
        }
    }

    private void adjustFullscreenMode() {
        Window window = getDialog().getWindow();
        if (window == null) return;

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    protected abstract String getTagId();

    @LayoutRes
    protected abstract int getLayoutResId();

    protected abstract void initUiComponents(@NonNull LayoutInflater inflater, View rootView);
}
