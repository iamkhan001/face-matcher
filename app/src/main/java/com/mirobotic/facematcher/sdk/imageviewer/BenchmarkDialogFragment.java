/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.imageviewer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.cyberlink.faceme.DetectionMode;
import com.cyberlink.facemedemo.ui.BaseDialogFragment;
import com.cyberlink.facemedemo.ui.CLToast;
import com.cyberlink.facemedemo.ui.UiSettings;
import com.mirobotic.facematcher.BuildConfig;
import com.mirobotic.facematcher.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BenchmarkDialogFragment<B extends BenchmarkDialogFragment.Broker> extends BaseDialogFragment<B> {
    private static final String TAG = "BenchmarkDialogFragment";

    private static final String ARG_BATCH_MODE = "batch";

    public interface Broker extends IBroker {
        UiSettings getUiSettings();

        List<ImageItem> getImageItems();

        void onItemBegin(ImageItem imageItem);
        void onBitmapLoaded(ImageItem imageItem);
        void onBitmapFailed(ImageItem imageItem);

        default void onBenchmarkBegin() {}
        default void onBenchmarkCancelled() {}
        default void onBenchmarkFailed() {}
        default void onBenchmarkEnd() {}
    }

    private boolean isBatchMode = false;
    private int testRound = 1;

    private View layoutTestRound;
    private View btnIncTestRound;
    private View btnDecTestRound;
    private TextView txtTestRound;

    private TextView txtProgressBarTitle;
    private ProgressBar progressRound;
    private TextView txtProgressRound;
    private TextView txtRoundItem;
    private ProgressBar progressTest;
    private TextView txtTestItem;
    private TextView txtTestProgress;

    private View btnTestClose;
    private View btnTestStart;
    private View btnTestCancel;

    private int roundIndex = 0;
    private BenchmarkTask benchmarkTask = null;

    private final ArrayList<File> outputFiles = new ArrayList<>();

    public static BenchmarkDialogFragment newInstance(boolean batchMode) {
        BenchmarkDialogFragment fragment = new BenchmarkDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_BATCH_MODE, batchMode);
        args.putBoolean(ARG_FULLSCREEN, false);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            isBatchMode = arguments.getBoolean(ARG_BATCH_MODE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setCancelable(false);

        if (!isBatchMode) startBenchmarkTesting(); // Single mode.
    }

    @Override
    protected String getTagId() {
        return TAG;
    }

    @LayoutRes
    protected int getLayoutResId() {
        return R.layout.fragment_benchmark;
    }

    @Override
    protected void initUiComponents(@NonNull LayoutInflater inflater, View rootView) {
        initRoundTest(rootView);
        initProgressBar(rootView);
        initButtons(rootView);
        initBatchMode(rootView);
    }

    private void initRoundTest(View rootView) {
        layoutTestRound = rootView.findViewById(R.id.layoutTestRound);
        btnIncTestRound = rootView.findViewById(R.id.btnIncTestRound);
        btnDecTestRound = rootView.findViewById(R.id.btnDecTestRound);
        txtTestRound = rootView.findViewById(R.id.txtTestRound);

        btnIncTestRound.setOnClickListener((v) -> updateTestRound(testRound + 1));
        btnDecTestRound.setOnClickListener((v) -> updateTestRound(testRound - 1));

        updateTestRound(testRound);
    }

    private void updateTestRound(int testRound) {
        if (testRound <= 0) return;

        this.testRound = testRound;
        roundIndex = 0;
        btnIncTestRound.setEnabled(testRound < 999);
        btnDecTestRound.setEnabled(testRound > 1);
        txtTestRound.setText(String.valueOf(testRound));
    }

    private void initProgressBar(View rootView) {
        txtProgressBarTitle = rootView.findViewById(R.id.txtProgressBarTitle);
        progressRound = rootView.findViewById(R.id.progressRound);
        txtProgressRound = rootView.findViewById(R.id.txtProgressRound);
        txtRoundItem = rootView.findViewById(R.id.txtRoundItem);
        progressTest = rootView.findViewById(R.id.progressTest);
        txtTestItem = rootView.findViewById(R.id.txtTestItem);
        txtTestProgress = rootView.findViewById(R.id.txtTestProgress);
    }

    private void initButtons(View rootView) {
        btnTestClose = rootView.findViewById(R.id.btnTestClose);
        btnTestStart = rootView.findViewById(R.id.btnTestStart);
        btnTestCancel = rootView.findViewById(R.id.btnTestCancel);

        btnTestClose.setOnClickListener((v) -> dismiss());
        btnTestStart.setOnClickListener((v) -> startBenchmarkTesting()); // Batch mode.
        btnTestCancel.setOnClickListener((v) -> cancelBenchmarkTesting());
    }

    private void initBatchMode(View rootView) {
        layoutTestRound.setVisibility(isBatchMode ? View.VISIBLE : View.GONE);

        rootView.findViewById(R.id.layoutProgressRound).setVisibility(isBatchMode ? View.VISIBLE : View.GONE);

        btnTestClose.setVisibility(isBatchMode ? View.VISIBLE : View.GONE);
        btnTestStart.setVisibility(isBatchMode ? View.VISIBLE : View.GONE);
        btnTestCancel.setVisibility(isBatchMode ? View.GONE : View.VISIBLE);
    }

    private void switchButtons(boolean runTest) {
        layoutTestRound.setVisibility(isBatchMode && !runTest ? View.VISIBLE : View.GONE);
        txtProgressBarTitle.setVisibility(isBatchMode && !runTest ? View.GONE : View.VISIBLE);

        btnTestClose.setVisibility(runTest ? View.GONE : View.VISIBLE);
        btnTestStart.setVisibility(!isBatchMode || runTest ? View.GONE : View.VISIBLE);
        btnTestCancel.setVisibility(runTest ? View.VISIBLE : View.GONE);

        if (runTest) {
            updateAverageProgress(0);
            progressRound.setMax(testRound * 100);
        }
    }

    private void startBenchmarkTesting() {
        roundIndex = 0;
        outputFiles.clear();
        runNextTesting();

        switchButtons(true);
    }

    private void cancelBenchmarkTesting() {
        roundIndex = 0;
        deleteOutputFiles();
        cancelTask();
        broker.onBenchmarkCancelled();

        if (isBatchMode) {
            txtRoundItem.setText("");
            txtTestItem.setText("");
            updateAverageProgress(0);
            switchButtons(false);
        } else {
            dismiss();
        }
    }

    private void deleteOutputFiles() {
        for (File outputFile : outputFiles) {
            outputFile.delete();
        }
    }

    private void cancelTask() {
        if (benchmarkTask != null) {
            benchmarkTask.cancel();
            benchmarkTask = null;
        }
    }

    private void updateAverageProgress(int progress) {
        int roundProgress = roundIndex * 100 + progress;
        progressRound.setProgress(roundProgress);
        txtProgressRound.setText((roundProgress * 100 / progressRound.getMax()) + "%");
        progressTest.setProgress(progress);
        txtTestProgress.setText(progress + "%");
    }

    private void runNextTesting() {
        cancelTask();

        Context context = getContext();
        if (context == null || roundIndex >= testRound) {
            switchButtons(false);
            return;
        }

        updateAverageProgress(0);
        txtRoundItem.setText(roundIndex + "/" + testRound);
        txtTestItem.setText("");
        benchmarkTask = new BenchmarkTask(context, broker.getImageItems(), broker.getUiSettings(), new BenchmarkTask.Observer() {
            @Override
            public void onItemBegin(ImageItem imageItem) {
                txtTestItem.setText(imageItem.file.getName());
                broker.onItemBegin(imageItem);
            }

            @Override
            public void onBitmapLoaded(ImageItem imageItem) {
                broker.onBitmapLoaded(imageItem);
            }

            @Override
            public void onBitmapFailed(ImageItem imageItem) {
                broker.onBitmapFailed(imageItem);
            }

            @Override
            public void onBenchmarkProgress(int progress) {
                updateAverageProgress(progress);
            }

            @Override
            public void onBenchmarkFailed(String message) {
                CLToast.show(context, message);
                switchButtons(false);
                broker.onBenchmarkFailed();
            }

            @Override
            public void onBenchmarkEnd(File outputFile, double avgTotal, double avgNormalDetect, double avgFastDetect, double avgExtract) {
                roundIndex++;
                outputFiles.add(outputFile);
                if (roundIndex >= testRound) {
                    txtRoundItem.setText(roundIndex + "/" + testRound);
                    if (isBatchMode) {
                        CLToast.show(context, "DONE!");
                    } else {
                        txtProgressBarTitle.setText("Benchmark is finished.");
                        String toastMsg = " Average duration of each stage are"
                                + "\ndetectN: " + String.format(Locale.US, "%.1fms", avgNormalDetect);
                        if (broker.getUiSettings().getDetectionMode() == DetectionMode.FAST) {
                            toastMsg += "\ndetectF: " + String.format(Locale.US, "%.1fms", avgFastDetect);
                        }
                        toastMsg += "\nextract: " + String.format(Locale.US, "%.1fms", avgExtract)
                                + "\ntotal: " + String.format(Locale.US, "%.1fms", avgTotal);
                        CLToast.showLong(context, toastMsg);

                        openOutputFile(outputFile);
                    }
                }

                broker.onBenchmarkEnd();
                runNextTesting();
            }
        });
        benchmarkTask.start();
    }

    private void openOutputFile(File outputFile) {
        Context context = getContext();
        if (context == null) return;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Uri outputUri = FileProvider.getUriForFile(context,
                BuildConfig.APPLICATION_ID + ".provider", outputFile);
        intent.setDataAndType(outputUri, "text/csv");

        PackageManager pm = context.getPackageManager();
        if (intent.resolveActivity(pm) != null) {
            startActivity(intent);
        }
    }
}
