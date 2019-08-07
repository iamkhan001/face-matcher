/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.imageviewer;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.List;

public class ImagePagerAdapter extends FragmentStatePagerAdapter {

    private final List<ImageItem> dataSet;

    ImagePagerAdapter(Fragment fragment, List<ImageItem> dataSet) {
        // Note: Initialize with the child fragment manager.
        super(fragment.getChildFragmentManager());
        this.dataSet = dataSet;
    }

    @Override
    public int getCount() {
        return dataSet.size();
    }

    @Override
    public Fragment getItem(int position) {
        return ImageViewerFragment.newInstance(position);
    }
}
