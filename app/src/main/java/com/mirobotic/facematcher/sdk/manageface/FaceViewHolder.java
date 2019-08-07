/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.manageface;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.mirobotic.facematcher.R;


class FaceViewHolder extends RecyclerView.ViewHolder {

    final ImageView imgFace;
    final TextView txtFaceId;
    final TextView txtFaceName;

    FaceViewHolder(View itemView) {
        super(itemView);
        imgFace = itemView.findViewById(R.id.faceThumb);
        txtFaceId = itemView.findViewById(R.id.faceId);
        txtFaceName= itemView.findViewById(R.id.faceName);
    }

}
