/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.cyberlink.facemedemo.ui;

import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.cyberlink.facemedemo.extension.BuildConfig;
import com.cyberlink.facemedemo.extension.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SettingsFragment<B extends SettingsFragment.Broker> extends BaseDialogFragment<B> {
    private static final String TAG = "SettingsFragment";

    private static final String ARG_TRANSPARENT = "transparent";

    private static final int MIN_MIN_FACE_WIDTH = 16;
    private static final int MAX_MIN_FACE_WIDTH = 480;

    public interface Broker extends IBroker {
        UiSettings getUiSettings();

        default void onSettingsChanged(boolean needRebuild) {}
    }

    private boolean isTransparentBackground = true;

    private SeekBar seekBarMinFaceWidth;
    private View btnAddMinFaceWidth;
    private View btnRemoveMinFaceWidth;
    private TextView txtMinFaceWidth;

    private SeekBar seekBarEngineThreads;
    private TextView txtEngineThreads;

    private RadioGroup radioDetectMethod;
    private RadioGroup radioExtractModel;
    private RadioGroup radioPrecisionLevel;
    private RadioGroup radioDetectSpeedLevel;
    private RadioGroup radioFastDetection;

    private Switch btnLandmark;

    private Switch btnFeatures;
    private View layoutFeatureSet;
    private Switch btnAge;
    private Switch btnAgeRange;
    private View layoutAgeRange;
    private Switch btnGender;
    private Switch btnEmotion;
    private Switch btnPose;

    public static SettingsFragment newInstance(boolean transparent, boolean fullscreen) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_TRANSPARENT, transparent);
        args.putBoolean(ARG_FULLSCREEN, fullscreen);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            isTransparentBackground = arguments.getBoolean(ARG_TRANSPARENT);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        updateUiSettings();
    }

    @Override
    public void onResume() {
        super.onResume();

        int height = getResources().getDimensionPixelSize(R.dimen.dialog_max_height);
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, height);
    }

    @Override
    protected String getTagId() {
        return TAG;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_settings;
    }

    @Override
    protected void initUiComponents(@NonNull LayoutInflater inflater, View rootView) {
        if (!isTransparentBackground) {
            rootView.setBackgroundResource(R.drawable.bg_rounded_grey);
        }

        initSdkVersion(rootView);
        initMinFaceWidth(rootView);
        initEngineThreads(rootView);
        initDetectMethod(rootView);
        initExtractModel(rootView);
        initPrecisionLevel(rootView);
        initDetectSpeedLevel(rootView);
        initFastDetection(rootView);
        initLandmark(rootView);
        initFeatureSet(rootView);

        initBackupDatabase(rootView);
    }

    private static boolean theSameValue(boolean a, boolean b) {
        return a == b;
    }
    private static boolean theSameValue(int a, int b) {
        return a == b;
    }

    private void initSdkVersion(View rootView) {
        TextView txtSdkVersion = rootView.findViewById(R.id.txtSdkVersion);

        String version = getContext().getString(R.string.app_name) + " SDK: " + com.cyberlink.faceme.BuildConfig.VERSION_NAME; /* AAR */
        txtSdkVersion.setText(version);

        String extensionMode = " (" + BuildConfig.EXTENSION_MODE + ")";
        Spannable spanExtensionMode = new SpannableString(extensionMode);
        spanExtensionMode.setSpan(new ForegroundColorSpan(Color.GRAY), 0, extensionMode.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        txtSdkVersion.append(spanExtensionMode);
    }

    private void initMinFaceWidth(View rootView) {
        seekBarMinFaceWidth = rootView.findViewById(R.id.seekbarMinFaceWidth);
        btnAddMinFaceWidth = rootView.findViewById(R.id.btnAddMinFaceWidth);
        btnRemoveMinFaceWidth = rootView.findViewById(R.id.btnRemoveMinFaceWidth);
        txtMinFaceWidth = rootView.findViewById(R.id.txtMinFaceWidth);

        seekBarMinFaceWidth.setMax(MAX_MIN_FACE_WIDTH - MIN_MIN_FACE_WIDTH);
        btnAddMinFaceWidth.setOnClickListener((v) -> seekBarMinFaceWidth.setProgress(seekBarMinFaceWidth.getProgress() + 1));
        btnRemoveMinFaceWidth.setOnClickListener((v) -> seekBarMinFaceWidth.setProgress(seekBarMinFaceWidth.getProgress() - 1));

        seekBarMinFaceWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateMinFaceWidthValue(progress);

                int newMinFaceWidth = progress + MIN_MIN_FACE_WIDTH;
                if (theSameValue(newMinFaceWidth, broker.getUiSettings().getMinFaceWidth())) return;

                broker.getUiSettings().setMinFaceWidth(newMinFaceWidth);
                broker.onSettingsChanged(false);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateMinFaceWidthValue(int seekBarProgress) {
        txtMinFaceWidth.setText(String.valueOf(seekBarProgress + MIN_MIN_FACE_WIDTH));
        btnAddMinFaceWidth.setEnabled(seekBarProgress < seekBarMinFaceWidth.getMax());
        btnRemoveMinFaceWidth.setEnabled(seekBarProgress > 0);
    }

    private void initEngineThreads(View rootView) {
        seekBarEngineThreads = rootView.findViewById(R.id.seekbarEngineThreads);
        txtEngineThreads = rootView.findViewById(R.id.txtEngineThreads);

        int cpuCounts = Runtime.getRuntime().availableProcessors();
        if (cpuCounts == 1) seekBarEngineThreads.setEnabled(false);
        seekBarEngineThreads.setMax(cpuCounts - 1);
        seekBarEngineThreads.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateEngineThreadValue(progress);

                int newEngineThreads = progress + 1;
                if (theSameValue(newEngineThreads, broker.getUiSettings().getEngineThreads())) return;

                broker.getUiSettings().setEngineThreads(newEngineThreads);
                broker.onSettingsChanged(true);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateEngineThreadValue(int seekBarProgress) {
        txtEngineThreads.setText(String.valueOf(seekBarProgress + 1));
    }

    private void initDetectMethod(View rootView) {
        radioDetectMethod = rootView.findViewById(R.id.radioDetectMethod);
        radioDetectMethod.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            Object tag = radioGroup.findViewById(checkedId).getTag();
            try {
                int newFlag = Integer.parseInt((String) tag);
                if (theSameValue(newFlag, broker.getUiSettings().getEnginePreference())) return;

                broker.getUiSettings().setEnginePreference(newFlag);
                broker.onSettingsChanged(true);
            } catch (NumberFormatException ignored) {}
        });
    }

    private void updateDetectMethod(int flag) {
        int childCount = radioDetectMethod.getChildCount();
        for (int idx = 0; idx < childCount; idx++) {
            View child = radioDetectMethod.getChildAt(idx);
            if (!(child instanceof RadioButton)) return;
            if (!(child.getTag() instanceof String)) return;

            try {
                if (Integer.parseInt((String) child.getTag()) == flag) {
                    ((RadioButton) child).setChecked(true);
                    break;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private void initExtractModel(View rootView) {
        radioExtractModel = rootView.findViewById(R.id.radioExtractModel);
        radioExtractModel.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            Object tag = radioGroup.findViewById(checkedId).getTag();
            try {
                int newLevel = Integer.parseInt((String) tag);
                if (theSameValue(newLevel, broker.getUiSettings().getExtractModel())) return;

                broker.getUiSettings().setExtractModel(newLevel);
                broker.onSettingsChanged(true);
            } catch (NumberFormatException ignored) {}
        });
    }

    private void updateExtractModel(int method) {
        int childCount = radioExtractModel.getChildCount();
        for (int idx = 0; idx < childCount; idx++) {
            View child = radioExtractModel.getChildAt(idx);
            if (!(child instanceof RadioButton)) return;
            if (!(child.getTag() instanceof String)) return;

            try {
                if (Integer.parseInt((String) child.getTag()) == method) {
                    ((RadioButton) child).setChecked(true);
                    break;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    /**
     * @see com.cyberlink.faceme.PrecisionLevel
     */
    private void initPrecisionLevel(View rootView) {
        radioPrecisionLevel = rootView.findViewById(R.id.radioPrecisionLevel);
        radioPrecisionLevel.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            Object tag = radioGroup.findViewById(checkedId).getTag();
            try {
                int newLevel = Integer.parseInt((String) tag);
                if (theSameValue(newLevel, broker.getUiSettings().getPrecisionLevel())) return;

                broker.getUiSettings().setPrecisionLevel(newLevel);
                broker.onSettingsChanged(false);
            } catch (NumberFormatException ignored) {}
        });
    }

    private void updatePrecisionLevel(int level) {
        int childCount = radioPrecisionLevel.getChildCount();
        for (int idx = 0; idx < childCount; idx++) {
            View child = radioPrecisionLevel.getChildAt(idx);
            if (!(child instanceof RadioButton)) return;
            if (!(child.getTag() instanceof String)) return;

            try {
                if (Integer.parseInt((String) child.getTag()) == level) {
                    ((RadioButton) child).setChecked(true);
                    break;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private void initDetectSpeedLevel(View rootView) {
        radioDetectSpeedLevel = rootView.findViewById(R.id.radioDetectSpeedLevel);
        radioDetectSpeedLevel.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            Object tag = radioGroup.findViewById(checkedId).getTag();
            try {
                int newLevel = Integer.parseInt((String) tag);
                if (theSameValue(newLevel, broker.getUiSettings().getDetectSpeedLevel())) return;

                broker.getUiSettings().setDetectSpeedLevel(newLevel);
                broker.onSettingsChanged(false);
            } catch (NumberFormatException ignored) {}
        });

    }

    private void updateDetectSpeedLevel(int level) {
        int childCount = radioDetectSpeedLevel.getChildCount();
        for (int idx = 0; idx < childCount; idx++) {
            View child = radioDetectSpeedLevel.getChildAt(idx);
            if (!(child instanceof RadioButton)) return;
            if (!(child.getTag() instanceof String)) return;

            try {
                if (Integer.parseInt((String) child.getTag()) == level) {
                    ((RadioButton) child).setChecked(true);
                    break;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private void initFastDetection(View rootView) {
        radioFastDetection = rootView.findViewById(R.id.radioFastDetection);
        radioFastDetection.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            Object tag = radioGroup.findViewById(checkedId).getTag();
            try {
                int newMode = Integer.parseInt((String) tag);
                if (theSameValue(newMode, broker.getUiSettings().getDetectionMode())) return;

                broker.getUiSettings().setDetectionMode(newMode);
                broker.onSettingsChanged(false);
            } catch (NumberFormatException ignored) {}
        });
    }

    private void updateFastDetection(int mode) {
        int childCount = radioFastDetection.getChildCount();
        for (int idx = 0; idx < childCount; idx++) {
            View child = radioFastDetection.getChildAt(idx);
            if (!(child instanceof RadioButton)) return;
            if (!(child.getTag() instanceof String)) return;

            try {
                if (Integer.parseInt((String) child.getTag()) == mode) {
                    ((RadioButton) child).setChecked(true);
                    break;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private void initLandmark(View rootView) {
        btnLandmark = rootView.findViewById(R.id.btnLandmark);
        btnLandmark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (theSameValue(isChecked, broker.getUiSettings().isShowLandmark())) return;

            broker.getUiSettings().setShowLandmark(isChecked);
            broker.onSettingsChanged(false);
        });
    }

    private void initFeatureSet(View rootView) {
        btnFeatures = rootView.findViewById(R.id.btnFeatures);
        layoutFeatureSet = rootView.findViewById(R.id.layoutFeatureSet);
        btnFeatures.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (theSameValue(isChecked, broker.getUiSettings().isShowFeatures())) return;

            broker.getUiSettings().setShowFeatures(isChecked);
            broker.onSettingsChanged(false);
            layoutFeatureSet.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        btnAge = rootView.findViewById(R.id.btnAge);
        btnAgeRange = rootView.findViewById(R.id.btnAgeRange);
        layoutAgeRange = rootView.findViewById(R.id.layoutAgeRange);
        btnAge.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (theSameValue(isChecked, broker.getUiSettings().isShowAge())) return;

            broker.getUiSettings().setShowAge(isChecked);
            broker.onSettingsChanged(false);
            layoutAgeRange.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        btnAgeRange.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (theSameValue(isChecked, broker.getUiSettings().isAgeInRange())) return;

            broker.getUiSettings().setAgeInRange(isChecked);
            broker.onSettingsChanged(false);
        });

        btnGender = rootView.findViewById(R.id.btnGender);
        btnGender.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (theSameValue(isChecked, broker.getUiSettings().isShowGender())) return;

            broker.getUiSettings().setShowGender(isChecked);
            broker.onSettingsChanged(false);
        });

        btnEmotion = rootView.findViewById(R.id.btnEmotion);
        btnEmotion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (theSameValue(isChecked, broker.getUiSettings().isShowEmotion())) return;

            broker.getUiSettings().setShowEmotion(isChecked);
            broker.onSettingsChanged(false);
        });

        btnPose = rootView.findViewById(R.id.btnPose);
        btnPose.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (theSameValue(isChecked, broker.getUiSettings().isShowPose())) return;

            broker.getUiSettings().setShowPose(isChecked);
            broker.onSettingsChanged(false);
        });
    }

    private void initBackupDatabase(View rootView) {
        rootView.findViewById(R.id.btnFaceDbImport).setOnClickListener((view) -> backupDatabase(true));
        rootView.findViewById(R.id.btnFaceDbExport).setOnClickListener((view) -> backupDatabase(false));
    }

    private void backupDatabase(boolean isImport) {
        File src, dst;
        if (isImport) {
            src = new File(Environment.getExternalStorageDirectory(), "FaceMe/faceMe.fdb");
            dst = new File(getContext().getFilesDir(), "CyberLink/FaceMeSDK/db/faceMe.fdb");
        } else {
            src = new File(getContext().getFilesDir(), "CyberLink/FaceMeSDK/db/faceMe.fdb");
            dst = new File(Environment.getExternalStorageDirectory(), "FaceMe/faceMe_out.fdb");
        }
        // Lazy task, copy File on main thread.
        if (!src.exists()) {
            CLToast.show(getContext(), "Source database not found: " + src.getAbsolutePath());
            return;
        }
        if (!dst.getParentFile().exists()) dst.getParentFile().mkdirs();

        try {
            try (InputStream is = new BufferedInputStream(new FileInputStream(src));
                 OutputStream os = new BufferedOutputStream(new FileOutputStream(dst))) {

                byte[] buffer = new byte[4096]; // XXX: Usually 4KB per block.
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                if (isImport) {
                    broker.onSettingsChanged(true);
                } else {
                    String[] path = {dst.getAbsolutePath()};
                    String[] mimeType = {"application/octet-stream"};
                    MediaScannerConnection.scanFile(getContext(), path, mimeType, null);
                }
                CLToast.show(getContext(), "Done");
            }
        } catch (IOException e) {
            CLToast.show(getContext(), "Something went wrong while copy: " + e.getMessage());
        }
    }

    private void updateUiSettings() {
        UiSettings uiSettings = broker.getUiSettings();
        int progressValue;

        progressValue = uiSettings.getMinFaceWidth() - MIN_MIN_FACE_WIDTH;
        seekBarMinFaceWidth.setProgress(progressValue);
        updateMinFaceWidthValue(progressValue);

        progressValue = uiSettings.getEngineThreads() - 1;
        seekBarEngineThreads.setProgress(progressValue);
        updateEngineThreadValue(progressValue);

        updateDetectMethod(uiSettings.getEnginePreference());
        updateExtractModel(uiSettings.getExtractModel());
        updatePrecisionLevel(uiSettings.getPrecisionLevel());
        updateDetectSpeedLevel(uiSettings.getDetectSpeedLevel());

        updateFastDetection(broker.getUiSettings().getDetectionMode());

        btnLandmark.setChecked(uiSettings.isShowLandmark());

        btnFeatures.setChecked(uiSettings.isShowFeatures());
        if (!uiSettings.isShowFeatures()) {
            layoutFeatureSet.setVisibility(View.GONE);
        }
        btnAge.setChecked(uiSettings.isShowAge());
        if (!uiSettings.isShowAge()) {
            layoutAgeRange.setVisibility(View.GONE);
        }
        btnAgeRange.setChecked(uiSettings.isAgeInRange());
        btnGender.setChecked(uiSettings.isShowGender());
        btnEmotion.setChecked(uiSettings.isShowEmotion());
        btnPose.setChecked(uiSettings.isShowPose());
    }
}
