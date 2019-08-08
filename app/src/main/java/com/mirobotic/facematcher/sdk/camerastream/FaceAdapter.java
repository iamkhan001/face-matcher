/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.camerastream;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberlink.faceme.FaceAttribute;
import com.cyberlink.faceme.Gender;
import com.cyberlink.facemedemo.sdk.FaceHolder;
import com.cyberlink.facemedemo.ui.UiSettings;
import com.mirobotic.facematcher.R;

import java.util.ArrayList;
import java.util.List;

class FaceAdapter extends RecyclerView.Adapter<FaceViewHolder> {

    private final Context context;
    private final UiSettings uiSettings;
    private final ArrayList<FaceHolder> dataSet = new ArrayList<>();

    FaceAdapter(Context context, UiSettings uiSettings) {
        this.context = context;
        this.uiSettings = uiSettings;
    }

    @NonNull
    @Override
    public FaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.view_face_item, parent, false);
        return new FaceViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FaceViewHolder holder, int position) {
        FaceHolder faceHolder = dataSet.get(position);

        holder.imgFaceThumb.setImageBitmap(faceHolder.faceBitmap);
        holder.txtFaceName.setText(faceHolder.data.name);

        boolean noname = TextUtils.isEmpty(faceHolder.data.name);
        Boolean gender = determineGender(faceHolder.faceAttribute);

        holder.txtFaceName.setVisibility(noname ? View.GONE : View.VISIBLE);

        if (!noname && gender != null) {
            holder.itemView.setEnabled(true);
            holder.itemView.setSelected(!gender);
        } else {
            holder.itemView.setEnabled(false);
            holder.itemView.setSelected(!noname);
        }
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

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    void replaceAll(List<FaceHolder> faceHolders) {
        dataSet.clear();
        if (faceHolders != null) dataSet.addAll(faceHolders);
        notifyDataSetChanged();
    }
}
