/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.imageviewer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.mirobotic.facematcher.R;

public class ImageViewerFragment extends Fragment {
    static final String TAG = "ImageViewerFragment";

    private static final String KEY_POSITION = "com.cyberlink.facemedemo.key.position";
    private ImageBroker broker;

    public static ImageViewerFragment newInstance(int position) {
        ImageViewerFragment fragment = new ImageViewerFragment();
        Bundle argument = new Bundle();
        argument.putInt(KEY_POSITION, position);
        fragment.setArguments(argument);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View itemView = inflater.inflate(R.layout.fragment_image_viewer, container, false);

        Bundle arguments = getArguments();
        int position = arguments.getInt(KEY_POSITION, 0);

        ImageItem imageItem = broker.getImageItems().get(position);

        ImageView imgViewer = itemView.findViewById(R.id.imgViewer);
        imgViewer.setTransitionName(imageItem.file.getAbsolutePath());

        Glide.with(this)
                .load(imageItem.file)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        getParentFragment().startPostponedEnterTransition();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        getParentFragment().startPostponedEnterTransition();
                        return false;
                    }
                })
                .into(imgViewer);

        return itemView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof ImageBroker) {
            broker = (ImageBroker) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        broker = null;
    }
}