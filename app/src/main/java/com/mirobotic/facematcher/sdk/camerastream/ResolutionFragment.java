/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.camerastream;

import android.hardware.Camera;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.cyberlink.facemedemo.ui.BaseDialogFragment;
import com.mirobotic.facematcher.R;

import java.util.List;

public class ResolutionFragment<B extends ResolutionFragment.Broker> extends BaseDialogFragment<B> {
    private static final String TAG = "ResolutionFragment";

    public interface Broker extends IBroker {
        Size getCurrentResolution();

        <S> List<S> getResolutions();

        void onResolutionChanged(Size size);
    }

    private View selectedView = null;

    ViewGroup listResolutions;

    public static ResolutionFragment newInstance() {
        ResolutionFragment fragment = new ResolutionFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FULLSCREEN, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();

        updateUiSettings();
    }

    @Override
    protected String getTagId() {
        return TAG;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_resolution;
    }

    @Override
    protected void initUiComponents(@NonNull LayoutInflater inflater, View rootView) {
        listResolutions = rootView.findViewById(R.id.listResolutions);

        List resolutions = broker.getResolutions();
        if (resolutions == null) return;

        View.OnClickListener listener = (view) -> {
            Object tag = view.getTag(R.id.tag_Size);
            if (tag instanceof Camera.Size) {
                Camera.Size size = (Camera.Size) tag;
                broker.onResolutionChanged(new Size(size.width, size.height));
            } else if (tag instanceof Size) {
                broker.onResolutionChanged((Size) tag);
            }
            if (selectedView != null) selectedView.setSelected(false);

            view.setSelected(true);
            selectedView = view;

        };

        for (Object item : resolutions) {
            View itemView = inflater.inflate(R.layout.view_resolution_item, listResolutions, false);
            TextView txtResolution = itemView.findViewById(R.id.txtResolution);

            if (item instanceof Camera.Size) {
                Camera.Size size = (Camera.Size) item;
                txtResolution.setText(size.width + "x" + size.height);
                itemView.setTag(R.id.tag_Size, new Size(size.width, size.height));
            }

            itemView.setOnClickListener(listener);
            listResolutions.addView(itemView);
        }
    }

    private void updateUiSettings() {
        Size previewSize = broker.getCurrentResolution();
        int childCount = listResolutions.getChildCount();
        for (int idx = 0; idx < childCount; idx++) {
            View child = listResolutions.getChildAt(idx);
            if (previewSize.equals(child.getTag(R.id.tag_Size))) {
                child.setSelected(true);
                selectedView = child;
                break;
            }
        }
    }
}
