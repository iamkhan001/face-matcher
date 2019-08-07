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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.mirobotic.facematcher.R;

import java.util.ArrayList;
import java.util.List;

class CollectionAdapter extends RecyclerView.Adapter<CollectionViewHolder> {

    private final Context context;
    private final ArrayList<CollectionHolder> dataSet = new ArrayList<>();
    private IManageCollectionListener manageCollectionListener = null;
    private IFetchFaceThumbnail fetchFaceThumbnail = null;

    CollectionAdapter(Context context, IFetchFaceThumbnail fetchFaceThumbnail) {
        this.context = context;
        this.fetchFaceThumbnail = fetchFaceThumbnail;
    }

    @NonNull
    @Override
    public CollectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.view_collection_item, parent, false);
        return new CollectionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionViewHolder holder, int position) {
        final CollectionHolder collectionHolder = dataSet.get(position);

        Bitmap collectionThumbnail = null;
        if (fetchFaceThumbnail != null && collectionHolder.faceIds.size() > 0) {
            collectionThumbnail = fetchFaceThumbnail.OnFetchFaceThumbnail(collectionHolder.faceIds.get(0));
        }
        if (collectionThumbnail != null) {
            holder.imgCollectionThumb.setImageBitmap(collectionThumbnail);
        } else {
            holder.imgCollectionThumb.setImageResource(R.drawable.ic_face_n);
        }

        String collectionName = collectionHolder.collectionName + " (" + collectionHolder.faceIds.size() + ")";
        String collectionInfo = String.format("cId : %-10s", collectionHolder.collectionId);
        if (collectionHolder.faceIds.size() > 0)
            collectionInfo += "fId : " + collectionHolder.faceIds.get(0);
        holder.txtCollectionName.setText(collectionName);
        holder.txtCollectionInfo.setText(collectionInfo);

        PopupMenu.OnMenuItemClickListener onMenuItemClickListener = item -> {
            switch (item.getItemId()) {
                case R.id.menuEditFaceCollection:
                    manageCollectionListener.onEditCollection(collectionHolder);
                    return true;
                case R.id.menuDeleteFaceCollection:
                    manageCollectionListener.onDeleteCollection(collectionHolder);
                    return true;
            }
            return false;
        };

        holder.itemView.setOnClickListener(view -> {
            manageCollectionListener.onClickCollection(collectionHolder);
        });

        holder.btnMore.setOnClickListener((view) -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.btnMore);
            popupMenu.setOnMenuItemClickListener(onMenuItemClickListener);
            popupMenu.inflate(R.menu.menu_face_collection);
            popupMenu.show();
        });
    }

    void SetManageFaceCollectionListener(IManageCollectionListener manageCollectionListener) {
        this.manageCollectionListener = manageCollectionListener;
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    @Override
    public long getItemId(int position) {
        return dataSet.get(position).collectionId;
    }

    void replaceAll(List<CollectionHolder> collectionHolders) {
        dataSet.clear();
        if (collectionHolders != null) dataSet.addAll(collectionHolders);
        notifyDataSetChanged();
    }


}
