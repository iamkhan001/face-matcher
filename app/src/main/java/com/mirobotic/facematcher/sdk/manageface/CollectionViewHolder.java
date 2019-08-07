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


class CollectionViewHolder extends RecyclerView.ViewHolder {

    final ImageView imgCollectionThumb;
    final ImageView btnMore;
    final TextView txtCollectionName;
    final TextView txtCollectionInfo;

    CollectionViewHolder(View itemView) {
        super(itemView);

        imgCollectionThumb = itemView.findViewById(R.id.collectionThumb);
        txtCollectionName = itemView.findViewById(R.id.collectionName);
        txtCollectionInfo = itemView.findViewById(R.id.collectionInfo);
        btnMore = itemView.findViewById(R.id.btnMore);
    }

}
