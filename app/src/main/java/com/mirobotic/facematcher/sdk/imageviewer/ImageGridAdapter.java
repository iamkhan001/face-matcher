/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.imageviewer;

import android.graphics.drawable.Drawable;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.mirobotic.facematcher.R;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class ImageGridAdapter extends RecyclerView.Adapter<ImageViewHolder> {

    /**
     * A listener that is attached to all ViewHolders to handle image loading events and clicks.
     */
    interface ViewHolderListener {

        void onLoadCompleted(ImageView view, int adapterPosition);

        void onItemClicked(View view, int adapterPosition);
    }

    private final RequestManager requestManager;
    private final List<ImageItem> dataSet;
    private final ViewHolderListener viewHolderListener;

    ImageGridAdapter(ImageGridFragment fragment, List<ImageItem> dataSet) {
        this.requestManager = Glide.with(fragment);
        this.dataSet = dataSet;
        this.viewHolderListener = new ViewHolderListenerImpl(fragment);
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_image_item, parent, false);
        return new ImageViewHolder(itemView, viewHolderListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageItem imageItem = dataSet.get(position);
        holder.txtFileName.setText(imageItem.file.getName());
        holder.txtFileSize.setText(imageItem.getFileSize());
        if (imageItem.getResolution().getWidth() == 0) {
            holder.txtFileResolution.setVisibility(View.GONE);
        } else {
            holder.txtFileResolution.setVisibility(View.VISIBLE);
            holder.txtFileResolution.setText(imageItem.getResolution().toString());
        }
        holder.imgHighlightBorder.setVisibility(imageItem.selected ? View.VISIBLE : View.GONE);

        holder.imgFileThumb.setTransitionName(imageItem.file.getAbsolutePath());
        requestManager
                .load(imageItem.file)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        viewHolderListener.onLoadCompleted(holder.imgFileThumb, holder.getAdapterPosition());
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        viewHolderListener.onLoadCompleted(holder.imgFileThumb, holder.getAdapterPosition());
                        return false;
                    }
                })
                .into(holder.imgFileThumb);
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    void update(ImageItem imageItem) {
        if (imageItem == null) return;

        int position = dataSet.indexOf(imageItem);
        if (position == -1) return;

        notifyItemChanged(position);
    }

    /**
     * Default {@link ViewHolderListener} implementation.
     */
    private static class ViewHolderListenerImpl implements ViewHolderListener {

        private ImageGridFragment fragment;
        private AtomicBoolean enterTransitionStarted;

        ViewHolderListenerImpl(ImageGridFragment fragment) {
            this.fragment = fragment;
            this.enterTransitionStarted = new AtomicBoolean();
        }

        @Override
        public void onLoadCompleted(ImageView view, int position) {
            if (fragment.getBroker() == null) return;
            // Call startPostponedEnterTransition only when the 'selected' image loading is completed.
            if (fragment.getBroker().getCurrentPosition() != position) {
                return;
            }
            if (enterTransitionStarted.getAndSet(true)) {
                return;
            }
            fragment.startPostponedEnterTransition();
        }

        /**
         * Handles a view click by setting the current position to the given {@code position} and
         * starting a {@link  ImagePagerFragment} which displays the image at the position.
         *
         * @param view the clicked {@link ImageView} (the shared element view will be re-mapped at the
         * GridFragment's SharedElementCallback)
         * @param position the selected view position
         */
        @Override
        public void onItemClicked(View view, int position) {
            if (fragment.getBroker() == null) return;
            // Update the position.
            fragment.getBroker().setCurrentPosition(position);

            // Exclude the clicked card from the exit transition (e.g. the card will disappear immediately
            // instead of fading out with the rest to prevent an overlapping animation of fade and move).
            ((TransitionSet) fragment.getExitTransition()).excludeTarget(view, true);

            View transitioningView = view.findViewById(R.id.imgFileThumb);
            fragment.getFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true) // Optimize for shared element transition
                    .addSharedElement(transitioningView, transitioningView.getTransitionName())
                    .replace(R.id.image_fragment_container, new ImagePagerFragment(), ImagePagerFragment.TAG)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
