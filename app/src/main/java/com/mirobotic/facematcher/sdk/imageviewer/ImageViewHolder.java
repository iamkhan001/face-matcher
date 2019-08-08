/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.imageviewer;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.mirobotic.facematcher.R;


class ImageViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener {

    final ImageView imgFileThumb;
    final TextView txtFileName;
    final TextView txtFileSize;
    final TextView txtFileResolution;
    final View imgHighlightBorder;

    private final ImageGridAdapter.ViewHolderListener viewHolderListener;

    ImageViewHolder(View itemView, ImageGridAdapter.ViewHolderListener viewHolderListener) {
        super(itemView);

        imgFileThumb = itemView.findViewById(R.id.imgFileThumb);
        txtFileName = itemView.findViewById(R.id.txtFileName);
        txtFileSize = itemView.findViewById(R.id.txtFileSize);
        txtFileResolution = itemView.findViewById(R.id.txtFileResolution);
        imgHighlightBorder = itemView.findViewById(R.id.imgFileHighlightBorder);

        this.viewHolderListener = viewHolderListener;
        this.itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        viewHolderListener.onItemClicked(view, getAdapterPosition());
    }
}
