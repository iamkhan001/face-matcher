/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.manageface;

import android.Manifest;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberlink.facemedemo.sdk.FaceData;
import com.cyberlink.facemedemo.ui.BaseActivity;
import com.cyberlink.facemedemo.ui.CLToast;
import com.mirobotic.facematcher.R;

import java.util.ArrayList;

public class ManageFacesActivity extends BaseActivity implements IManageFaceListener, ManageHandler.ManageFaceHandlerCallback, IFetchFaceThumbnail {
    private static final String TAG = "ManageFacesActivity";

    @Override
    public Bitmap OnFetchFaceThumbnail(long faceId) {
        Bitmap collectionBitmap = null;
        if (manageHandler != null) {
            collectionBitmap = manageHandler.getFaceThumbnail(faceId);
        }
        return collectionBitmap;
    }

    private ActionBar actionBar;
    private FaceAdapter faceAdapter;
    private ManageHandler manageHandler;
    private TextView emptyHint;
    private TextView loadingHint;
    private RecyclerView faceListView;

    private final ArrayList<FaceData> faceDataItems = new ArrayList<>();
    private String collectionName;
    private long collectionId = -1;

    @Override
    protected String getTagId() {
        return TAG;
    }

    @LayoutRes
    @Override
    protected int getContentLayout() {
        return R.layout.activity_manage_faces;
    }

    @Override
    protected void initialize() {
        getInfo();
        initToolBar();
        initCollectionList();
    }

    private void getInfo() {
        Bundle extras = getIntent().getExtras();
        collectionId = extras.getLong(ManageCollectionsActivity.KEY_FACE_COLLECTION_ID);
    }

    private void initToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
    }

    private void initManagerFaceHandler() {
        if (manageHandler != null) manageHandler.releaseEngine();
        manageHandler = new ManageHandler(this, this);
    }

    @Override
    public void onInitEngine(boolean success) {
        if (success) {
            initCollectionName();
            updateFaceList();
        }
    }

    private void initCollectionName()
    {
        if (collectionId > 0 && manageHandler != null) {
            CollectionHolder collectionHolder = manageHandler.getCollectionInfo(collectionId);
            if (collectionHolder != null) {
                collectionName = collectionHolder.collectionName;
            }
        }
    }

    private void initCollectionList() {
        emptyHint = findViewById(R.id.emptyHint);
        loadingHint = findViewById(R.id.loadingHint);
        faceListView = findViewById(R.id.faceList);

        faceAdapter = new FaceAdapter(this, this);
        faceListView.setLayoutManager(new GridLayoutManager(this, 3));
        faceAdapter.SetManageFaceListener(this);
        faceListView.setAdapter(faceAdapter);
    }

    @Override
    protected String[] getRequiredPermissions() {
        return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    @Override
    protected String getPermissionString() {
        return "Storage";
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ||
                newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            faceAdapter.updateContainerSize();
            faceAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initManagerFaceHandler();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (manageHandler != null) {
            manageHandler.releaseEngine();
            manageHandler = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (manageHandler != null) {
            manageHandler.releaseEngine();
            manageHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void onDeleteFace(FaceData faceData) {
        LayoutInflater factory = LayoutInflater.from(this);
        final View thumbnailView = factory.inflate(R.layout.view_face_thumbnail, null);
        ImageView faceThumbnail = thumbnailView.findViewById(R.id.faceThumbnail);
        Bitmap thumbnail = manageHandler.getFaceThumbnail(faceData.faceId);
        if (thumbnail == null) {
            faceThumbnail.setImageResource(R.drawable.ic_face_n);
        } else {
            faceThumbnail.setImageBitmap(thumbnail);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Delete " + collectionName + " - " + faceData.faceId + " ?")
                .setView(thumbnailView)
                .setPositiveButton("OK", (dialog, which) -> {
                    boolean result = manageHandler.deleteFace(faceData.faceId);
                    if (result) {
                        CLToast.show(this, "Delete success");
                        updateFaceList();
                    } else {
                        CLToast.show(this, "Delete failed");
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                });
        builder.show();
    }

    private void updateFaceList() {
        loadingHint.setVisibility(View.VISIBLE);
        if(manageHandler != null) {
            ArrayList<Long> faceIds = manageHandler.getFaceIdsByCollectionId(collectionId);
            faceDataItems.clear();
            for (long faceId : faceIds) {
                faceDataItems.add(new FaceData(collectionId, faceId, collectionName));
            }

            faceAdapter.replaceAll(faceDataItems);
            if (faceDataItems.size() > 0) {
                emptyHint.setVisibility(View.GONE);
                faceListView.setVisibility(View.VISIBLE);
            } else {
                emptyHint.setVisibility(View.VISIBLE);
                faceListView.setVisibility(View.GONE);
            }
        }
        updateTitle();
        loadingHint.setVisibility(View.GONE);
    }

    private void updateTitle() {
        actionBar.setTitle(collectionName + " (" + faceAdapter.getItemCount() + ")");
    }
}
