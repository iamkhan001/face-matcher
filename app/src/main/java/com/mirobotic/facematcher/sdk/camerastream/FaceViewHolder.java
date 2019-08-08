/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.camerastream;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.mirobotic.facematcher.R;


class FaceViewHolder extends RecyclerView.ViewHolder {

    final ImageView imgFaceThumb;
    final TextView txtFaceName;

    FaceViewHolder(View itemView) {
        super(itemView);

        imgFaceThumb = itemView.findViewById(R.id.faceThumb);
        txtFaceName = itemView.findViewById(R.id.faceName);
    }
}
