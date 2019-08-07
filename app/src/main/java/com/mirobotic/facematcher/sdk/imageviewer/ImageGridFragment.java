/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.imageviewer;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.core.app.SharedElementCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mirobotic.facematcher.R;

import java.util.List;
import java.util.Map;

public class ImageGridFragment extends Fragment {
    static final String TAG = "ImageGridFragment";

    private static final int COLUMN_IN_PORTRAIT = 2;
    private static final int COLUMN_IN_LANDSCAPE = 3;

    private ImageBroker broker;
    private GridLayoutManager gridLayoutManager;
    private ImageGridAdapter imageGridAdapter;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = container.getContext();

        gridLayoutManager = new GridLayoutManager(context, getSpanCount(context.getResources().getConfiguration().orientation));
        imageGridAdapter = new ImageGridAdapter(this, broker.getImageItems());

        recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_image_items, container, false);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(imageGridAdapter);

        prepareTransitions();
        postponeEnterTransition();

        return recyclerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scrollToPosition();
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

    ImageBroker getBroker() {
        return broker;
    }

    private static int getSpanCount(int orientation) {
        return orientation == Configuration.ORIENTATION_LANDSCAPE ?
                COLUMN_IN_LANDSCAPE :
                COLUMN_IN_PORTRAIT;
    }

    void updateSpanCount(int orientation) {
        if (gridLayoutManager != null) gridLayoutManager.setSpanCount(getSpanCount(orientation));
    }

    void update(ImageItem imageItem) {
        if (imageGridAdapter != null) imageGridAdapter.update(imageItem);
    }

    void resetItems() {
        if (broker == null) return;
        if (imageGridAdapter != null) imageGridAdapter.notifyDataSetChanged();

        FragmentManager manager = getFragmentManager();
        if (manager == null) return;

        Fragment fragment = manager.findFragmentByTag(ImagePagerFragment.TAG);
        if (!(fragment instanceof ImagePagerFragment)) return;

        ((ImagePagerFragment) fragment).resetItems();
    }

    /**
     * Scrolls the recycler view to show the last viewed item in the grid. This is important when
     * navigating back from the grid.
     */
    private void scrollToPosition() {
        recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                recyclerView.removeOnLayoutChangeListener(this);
                final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                View viewAtPosition = layoutManager.findViewByPosition(broker.getCurrentPosition());
                // Scroll to position if the view for the current position is null (not currently part of
                // layout manager children), or it's not completely visible.
                if (viewAtPosition == null || layoutManager
                        .isViewPartiallyVisible(viewAtPosition, false, true)) {
                    recyclerView.post(() -> layoutManager.scrollToPosition(broker.getCurrentPosition()));
                }
            }
        });
    }

    /**
     * Prepares the shared element transition to the pager fragment, as well as the other transitions
     * that affect the flow.
     */
    private void prepareTransitions() {
        setExitTransition(TransitionInflater.from(getContext()).inflateTransition(R.transition.grid_exit_transition));

        // A similar mapping is set at the ImagePagerFragment with a setEnterSharedElementCallback.
        setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                // Locate the ViewHolder for the clicked position.
                RecyclerView.ViewHolder selectedViewHolder = recyclerView.findViewHolderForAdapterPosition(broker.getCurrentPosition());
                if (selectedViewHolder == null || selectedViewHolder.itemView == null) {
                    return;
                }

                // Map the first shared element name to the child ImageView.
                sharedElements.put(names.get(0), selectedViewHolder.itemView.findViewById(R.id.imgFileThumb));
            }
        });
    }
}
