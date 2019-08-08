/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.manageface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberlink.facemedemo.sdk.FaceData;
import com.mirobotic.facematcher.R;

import java.util.ArrayList;
import java.util.List;

class FaceAdapter extends RecyclerView.Adapter<FaceViewHolder> {

    private final Context context;
    private final ArrayList<FaceData> dataSet = new ArrayList<>();
    private IManageFaceListener manageFaceListener = null;
    private IFetchFaceThumbnail fetchFaceThumbnail = null;

    private int containerSize;

    FaceAdapter(Context context, IFetchFaceThumbnail fetchFaceThumbnail) {
        this.context = context;
        this.fetchFaceThumbnail = fetchFaceThumbnail;
        updateContainerSize();
    }

    public void updateContainerSize()
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        containerSize = size.x / 3;
    }

    @NonNull
    @Override
    public FaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.view_face_item, parent, false);
        itemView.getLayoutParams().width = containerSize;
        itemView.getLayoutParams().height = containerSize;
        itemView.setBackgroundColor(Color.TRANSPARENT);
        return new FaceViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FaceViewHolder holder, int position) {
        final FaceData faceData = dataSet.get(position);
        holder.itemView.getLayoutParams().width = containerSize;
        holder.itemView.getLayoutParams().height = containerSize;
        Bitmap collectionThumbnail = null;
        if (fetchFaceThumbnail != null && faceData.faceId != -1) {
            collectionThumbnail = fetchFaceThumbnail.OnFetchFaceThumbnail(faceData.faceId);
        }
        if (collectionThumbnail != null) {
            holder.imgFace.setImageBitmap(collectionThumbnail);
        } else {
            holder.imgFace.setImageResource(R.drawable.ic_face_n);
        }
        holder.txtFaceId.setText(faceData.faceId + "");
        holder.txtFaceId.setVisibility(View.VISIBLE);
        holder.txtFaceName.setVisibility(View.GONE);

        PopupMenu.OnMenuItemClickListener onMenuItemClickListener = item -> {
            switch (item.getItemId()) {
                case R.id.menuDeleteFace:
                    if(manageFaceListener != null) {
                        manageFaceListener.onDeleteFace(faceData);
                    }
                    return true;
            }
            return false;
        };

        holder.itemView.setOnClickListener((view) -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.itemView);
            popupMenu.setOnMenuItemClickListener(onMenuItemClickListener);
            popupMenu.inflate(R.menu.menu_face);
            popupMenu.show();
        });
    }

    void SetManageFaceListener(IManageFaceListener manageFaceListener) {
        this.manageFaceListener = manageFaceListener;
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    @Override
    public long getItemId(int position) {
        return dataSet.get(position).faceId;
    }

    void replaceAll(List<FaceData> collectionHolders) {
        dataSet.clear();
        if (collectionHolders != null) dataSet.addAll(collectionHolders);
        notifyDataSetChanged();
    }


}
