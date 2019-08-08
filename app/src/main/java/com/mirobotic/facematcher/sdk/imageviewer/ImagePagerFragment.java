/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.imageviewer;

import android.content.Context;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.core.app.SharedElementCallback;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.mirobotic.facematcher.R;

import java.util.List;
import java.util.Map;

public class ImagePagerFragment extends Fragment {
    static final String TAG = "ImagePagerFragment";

    private ImageBroker broker;
    private ImagePagerAdapter adapter;
    private ViewPager viewPager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        adapter = new ImagePagerAdapter(this, broker.getImageItems());

        viewPager = (ViewPager) inflater.inflate(R.layout.fragment_image_pager, container, false);
        viewPager.setAdapter(adapter);
        // Set the current position and add a listener that will update the selection coordinator when
        // paging the images.
        viewPager.setCurrentItem(broker.getCurrentPosition());
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                broker.setCurrentPosition(position);
            }
        });

        prepareSharedElementTransition();

        // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
        if (savedInstanceState == null) {
            postponeEnterTransition();
        }

        return viewPager;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof ImageBroker) {
            broker = (ImageBroker) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        broker = null;
    }

    void resetItems() {
        if (adapter == null) return;

        viewPager.setAdapter(adapter);
    }

    /**
     * Prepares the shared element transition from and back to the grid fragment.
     */
    private void prepareSharedElementTransition() {
        Transition transition = TransitionInflater.from(getContext()).inflateTransition(R.transition.image_shared_element_transition);
        setSharedElementEnterTransition(transition);

        // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
        setEnterSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                // Locate the image view at the primary fragment (the ImageFragment that is currently
                // visible). To locate the fragment, call instantiateItem with the selection position.
                // At this stage, the method will simply return the fragment at the position and will
                // not create a new one.
                Fragment currentFragment = (Fragment) adapter.instantiateItem(viewPager, broker.getCurrentPosition());
                View view = currentFragment.getView();
                if (view == null) return;

                // Map the first shared element name to the child ImageView.
                sharedElements.put(names.get(0), view.findViewById(R.id.imgViewer));
            }
        });
    }
}
