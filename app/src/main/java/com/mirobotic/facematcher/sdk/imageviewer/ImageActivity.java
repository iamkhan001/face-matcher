/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.imageviewer;

import android.Manifest;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.cyberlink.facemedemo.ui.BaseActivity;
import com.cyberlink.facemedemo.ui.CLToast;
import com.cyberlink.facemedemo.ui.LicenseInfoHandler;
import com.cyberlink.facemedemo.ui.SettingsFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mirobotic.facematcher.R;
import com.mirobotic.facematcher.utils.ASFUriHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageActivity extends BaseActivity implements
        BenchmarkDialogFragment.Broker, ImageBroker {
    private static final String TAG = "ImageActivity";

    private static final int DIRECTORY_REQUEST_CODE = 300;

    private static final String[] IMAGE_EXTENSION = {
            "jpg", "jpeg", "png"
    };

    private int currentPosition = -1;
    private ImageGridFragment imageGridFragment;

    private File benchmarkFolder;
    private ActionBar actionBar;
    private final ArrayList<ImageItem> imageItems = new ArrayList<>();

    @Override
    protected String getTagId() {
        return TAG;
    }

    @LayoutRes
    @Override
    protected int getContentLayout() {
        return R.layout.activity_image;
    }

    @Override
    protected void initialize() {
        // Initialize UI components
        initUiComponents();
    }

    @Override
    protected String[] getRequiredPermissions() {
        return new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE };
    }

    @Override
    protected String getPermissionString() {
        return "Storage";
    }

    @Override
    protected void onPermissionsGranted() {
        resetImagesDataSet();
    }

    private void initUiComponents() {
        initToolBar();
        initFragment();
        initFloatingActionButton();
        initImageAdapter();
    }

    private void initToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_image, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menuStartBenchmark: {
                LicenseInfoHandler.getLicenseInfo(this, (licenseInfo) -> {
                    BenchmarkDialogFragment.newInstance(true).show(getSupportFragmentManager());
                });
                return true;
            }
            case R.id.menuChooseFolder:
                openDirectoryChooser();
                return true;
            case R.id.menuSettings:
                SettingsFragment.newInstance(false, false).show(getSupportFragmentManager());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openDirectoryChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, DIRECTORY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DIRECTORY_REQUEST_CODE && resultCode == RESULT_OK) {
            File dir = ASFUriHelper.getDirectory(this, data);
            if (dir == null) {
                CLToast.show(this, "Folder is unavailable. Choose another one.");
                return;
            }

            benchmarkFolder = dir;
            uiSettings.setBenchmarkFolder(dir);
            resetImagesDataSet();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        imageGridFragment.updateSpanCount(newConfig.orientation);
    }

    private void initFloatingActionButton() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((view) -> {
            LicenseInfoHandler.getLicenseInfo(this, (licenseInfo) -> {
                BenchmarkDialogFragment.newInstance(false).show(getSupportFragmentManager());
            });
        });
    }

    private void initImageAdapter() {
        if (hasRequiredPermissions()) resetImagesDataSet();
    }

    private void resetImagesDataSet() {
        if (!hasRequiredPermissions()) return;

        long start = System.currentTimeMillis();

        if (benchmarkFolder == null) {
            benchmarkFolder = uiSettings.getBenchmarkFolder();
        }
        actionBar.setTitle(benchmarkFolder.getName());

        currentPosition = -1;
        imageItems.clear();
        imageItems.addAll(getAllImageFiles(benchmarkFolder));
        Collections.sort(imageItems, (item1, item2) -> item1.file.compareTo(item2.file));

        if (imageGridFragment != null) imageGridFragment.resetItems();

        Log.d(TAG, "resetImagesDataSet took " + (System.currentTimeMillis() - start) + "ms");
    }

    private void initFragment() {
        imageGridFragment = new ImageGridFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.image_fragment_container, imageGridFragment, ImageGridFragment.TAG)
                .commit();
    }

    @NonNull
    private static ArrayList<ImageItem> getAllImageFiles(@NonNull File rootDir) {
        File[] items = rootDir.listFiles((item) -> {
            String name = item.getName();

            if (name.startsWith(".")) return false; // Always ignore hidden item first.
            if (item.isDirectory()) return true; // Need to recursive sub-folders.
            if (!name.contains(".")) return false; // Must have file extension.

            for (String extension : IMAGE_EXTENSION) {
                if (name.toLowerCase().endsWith(extension)) return true;
            }

            return false;
        });

        ArrayList<ImageItem> files = new ArrayList<>();
        if (items != null) {
            for (File item : items) {
                if (item.isDirectory()) {
                    files.addAll(getAllImageFiles(item));
                } else if (item.isFile()) {
                    files.add(new ImageItem(item));
                }
            }
        }

        return files;
    }

    @Override
    public List<ImageItem> getImageItems() {
        return imageItems;
    }

    private ImageItem lastFocusItem = null;
    private void focusImageItem(ImageItem focusItem) {
        if (lastFocusItem != focusItem) {
            defocusLastImageItem();
        }

        focusItem.selected = true;
        imageGridFragment.update(focusItem);

        lastFocusItem = focusItem;
    }

    private void defocusLastImageItem() {
        if (lastFocusItem != null) {
            lastFocusItem.selected = false;
            imageGridFragment.update(lastFocusItem);
            lastFocusItem = null;
        }
    }

    @Override
    public void onItemBegin(ImageItem imageItem) {
        focusImageItem(imageItem);
    }

    @Override
    public void onBitmapLoaded(ImageItem imageItem) {
        focusImageItem(imageItem);
    }

    @Override
    public void onBitmapFailed(ImageItem imageItem) {
        focusImageItem(imageItem);
    }

    @Override
    public void onBenchmarkCancelled() {
        defocusLastImageItem();
    }

    @Override
    public void onBenchmarkFailed() {
        defocusLastImageItem();
    }

    @Override
    public void onBenchmarkEnd() {
        defocusLastImageItem();
    }

    @Override
    public int getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public void setCurrentPosition(int position) {
        this.currentPosition = position;
    }
}
