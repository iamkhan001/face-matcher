/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.validate;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.cyberlink.faceme.DetectionMode;
import com.cyberlink.faceme.DetectionSpeedLevel;
import com.cyberlink.faceme.EnginePreference;
import com.cyberlink.faceme.ExtractionModelSpeedLevel;
import com.cyberlink.faceme.PrecisionLevel;
import com.cyberlink.facemedemo.sdk.FaceMeRecognizerWrapper;
import com.cyberlink.facemedemo.ui.UiSettings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class ValidationTask extends Thread {
    private static final String TAG = "ValidationTask";

    private final Context context;
    private final UiSettings uiSettings;
    private final Observer observer;

    private final File validationFile;
    private final List<BaseTask> tasks = new ArrayList<>();

    private final Reporter reporter;

    private transient boolean isCancelled = false;
    private FaceMeRecognizerWrapper faceMeRecognizer;

    ValidationTask(Context context, UiSettings uiSettings, Observer observer) {
        super(TAG);

        this.context = context.getApplicationContext();
        this.uiSettings = uiSettings;
        this.observer = observer;

        this.validationFile = uiSettings.getValidationFile();
        this.reporter = new Reporter(context, validationFile, observer);
    }

    void cancel() {
        synchronized (tasks) {
            isCancelled = true;
            for (BaseTask task : tasks) {
                task.cancel();
            }
        }

        this.interrupt();
    }

    @Override
    public void run() {
        try {
            faceMeRecognizer = new FaceMeRecognizerWrapper(context);

            // 1st: LicenseManager.
            faceMeRecognizer.registerLicense();

            // 2nd: FaceMeRecognizer.
            faceMeRecognizer.initialize();
        } catch (Exception e) {
            Log.e(TAG, "Cannot setup FaceMe components", e);
            if (faceMeRecognizer != null) faceMeRecognizer.release();
            return;
        }

        faceMeRecognizer.configure();

        if (isCancelled) {
            faceMeRecognizer.release();
            return;
        }

        parseTasks();

        if (!isCancelled) reporter.onValidateBegin();

        validateItems();

        faceMeRecognizer.release();

        if (isCancelled) {
            reporter.closeWriterIfNeeded(true);
        } else {
            reporter.onValidateComplete();
        }
    }

    private void parseTasks() {
        List<BaseTask> tasks = TaskFactory.create(validationFile, observer);
        synchronized (this.tasks) {
            if (isCancelled) return;
            this.tasks.clear();
            this.tasks.addAll(tasks);
        }
    }

    private void validateItems() {
        for (BaseTask task : tasks) {
            if (isCancelled) return;
            if (task instanceof UnsupportedTask) {
                reporter.onSkip(task);
                continue;
            }

            try {
                boolean prepared = task.setUiSettings(uiSettings)
                        .setRecognizer(faceMeRecognizer)
                        .prepare();
                if (!prepared) {
                    reporter.onError(task, null);
                    continue;
                }
                if (isCancelled) return;

                String answerFromEngine = task.validate();
                reporter.onResult(task, TextUtils.isEmpty(answerFromEngine), task.getAnswer(), answerFromEngine);
            } catch (Exception e) {
                String msg = "Cannot validate task: " + e.getMessage();
                Log.e(TAG, msg, e);
                reporter.onError(task, msg);
            }
        }

        synchronized (this.tasks) {
            this.tasks.clear();
        }
    }

    private static class Reporter {
        private static final char COLUMN_SEPARATOR = ',';

        private final Context context;
        private final File validationFile;
        private final Observer observer;
        private final UiSettings uiSettings;

        private File outputFile;
        private Writer logWriter = null;

        private int countOfPass = 0;
        private int countOfFail = 0;
        private int countOfError = 0;
        private int countOfSkip = 0;

        Reporter(Context context, File validationFile, Observer observer) {
            this.context = context;
            this.validationFile = validationFile;
            this.observer = observer;

            this.uiSettings = new UiSettings(context);
        }

        private void createWriterIfNeeded() {
            if (logWriter != null) return;

            SimpleDateFormat filenameFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
            String filename = validationFile.getName();
            if (filename.lastIndexOf(".") > 0) {
                filename = filename.substring(0, filename.lastIndexOf("."));
            }
            outputFile = new File(validationFile.getParentFile(), filename + "_" + filenameFormat.format(new Date()) + ".csv");
            try {
                logWriter = new BufferedWriter(new FileWriter(outputFile, false), 2048);
            } catch (IOException e) {
                Log.e(TAG, "Cannot create log writer", e);
            }
        }

        private void closeWriterIfNeeded(boolean needDeleteFile) {
            if (logWriter == null) return;

            try {
                logWriter.flush();
                logWriter.close();

                if (needDeleteFile) {
                    outputFile.delete();
                } else {
                    String[] path = {outputFile.getAbsolutePath()};
                    String[] mimeType = {"text/csv"};
                    MediaScannerConnection.scanFile(context, path, mimeType, null);
                }
            } catch (IOException e) {
                Log.e(TAG, "Cannot close log writer", e);
            }
        }

        private void write(Object... args) {
            try {
                if (args != null) {
                    for (Object arg : args) {
                        if (arg == null) continue;
                        logWriter.write(String.valueOf(arg));
                        logWriter.write(COLUMN_SEPARATOR);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Cannot write log to file", e);
            }
        }
        private void writeLine(Object... args) {
            try {
                write(args);
                logWriter.write("\n");
            } catch (IOException e) {
                Log.e(TAG, "Cannot write log to file", e);
            }
        }

        private void writeHeader() {
            createWriterIfNeeded();
            writeLine("File", validationFile.getAbsolutePath());
            writeLine("Device", Build.VERSION.RELEASE, Build.DEVICE, Build.MODEL, Build.HARDWARE, Build.BRAND);
            writeLine("Time", new Date());
            writeLine("SDK Version", com.cyberlink.faceme.BuildConfig.VERSION_NAME);
            writeLine();
            writeLine("minFaceWidth", uiSettings.getMinFaceWidth());
            writeLine("EngineThreads", uiSettings.getEngineThreads());
            writeLine("DetectMethod", getDetectMethodString(uiSettings.getEnginePreference()));
            writeLine("ExtractModel", getExtractModelString(uiSettings.getExtractModel()));
            writeLine("PrecisionLevel", getPrecisionLevelString(uiSettings.getPrecisionLevel()));
            writeLine("DetectSpeedLevel", getDetectSpeedLevelString(uiSettings.getDetectSpeedLevel()));
            writeLine("DetectionMode", getDetectionModeString(uiSettings.getDetectionMode()));
            writeLine();
            writeLine("extractFeatureLandmark", uiSettings.isShowLandmark());
            writeLine("extractFeature", uiSettings.isShowFeatures());
            writeLine("extractAge", uiSettings.isShowAge());
            writeLine("extractGender", uiSettings.isShowGender());
            writeLine("extractEmotion", uiSettings.isShowEmotion());
            writeLine("extractPose", uiSettings.isShowPose());
            writeLine();

            write("Line#", "Result", "Answer", "Validation Text");

            writeLine();
        }

        private String getDetectMethodString(@EnginePreference.EEnginePreference int method) {
            switch (method) {
                case EnginePreference.PREFER_NONE:
                    return "DNN (more precise)";
                case EnginePreference.PREFER_FAST_DETECTION:
                    return "Non-DNN (faster)";
                default:
                    return "undefined(" + method + ")";
            }
        }

        private String getExtractModelString(@ExtractionModelSpeedLevel.EExtractionModelSpeedLevel int model) {
            switch (model) {
                case ExtractionModelSpeedLevel.HIGH:
                    return "High Precision (H1)";
                case ExtractionModelSpeedLevel.HIGH_ASIAN:
                    return "High Precision (H2)";
                case ExtractionModelSpeedLevel.STANDARD:
                    return "Standard Precision (S80)";
                default:
                    return "undefined(" + model + ")";
            }
        }

        private String getPrecisionLevelString(@PrecisionLevel.EPrecisionLevel int level) {
            switch (level) {
                case PrecisionLevel.LEVEL_1E6:
                    return "1e-6";
                case PrecisionLevel.LEVEL_1E5:
                    return "1e-5";
                case PrecisionLevel.LEVEL_1E4:
                    return "1e-4";
                case PrecisionLevel.LEVEL_1E3:
                    return "1e-3";
                case PrecisionLevel.LEVEL_1E2:
                    return "1e-2";
                default:
                    return "undefined(" + level + ")";
            }
        }

        private String getDetectSpeedLevelString(@DetectionSpeedLevel.EDetectionSpeedLevel int level) {
            switch (level) {
                case DetectionSpeedLevel.FAST:
                    return "Fast";
                case DetectionSpeedLevel.BALANCE:
                    return "Balance";
                case DetectionSpeedLevel.PRECISE:
                    return "Precise";
                default:
                    return "undefined(" + level + ")";
            }
        }

        private String getDetectionModeString(@DetectionMode.EDetectionMode int mode) {
            switch (mode) {
                case DetectionMode.NORMAL:
                    return "Normal";
                case DetectionMode.FAST:
                    return "Fast";
                default:
                    return "undefined(" + mode + ")";
            }
        }

        private void writeFooter() {
            writeLine();
            writeLine("Total", "PASS", "FAIL", "ERROR", "SKIP");
            writeLine((countOfPass + countOfFail + countOfError + countOfSkip),
                    countOfPass, countOfFail, countOfError, countOfSkip);
        }

        private void onValidateBegin() {
            observer.onInfo("");
            observer.onValidateBegin();
            writeHeader();
        }

        private void onSkip(BaseTask task) {
            writeLine(task.lineNum, "#SKIP", "", task.content);
            countOfSkip++;
            observer.onSkip(task);
        }

        private void onResult(BaseTask task, boolean isPass, String answerFromFile, String answerFromEngine) {
            write(task.lineNum);
            if (isPass) {
                write("PASS", "");
                countOfPass++;
            } else {
                write("FAIL", answerFromEngine);
                countOfFail++;
            }
            writeLine("\"" + task.content + "\"");

            observer.onResult(task, isPass, answerFromFile, answerFromEngine);
        }

        private void onError(BaseTask task, String msg) {
            writeLine(task.lineNum, "#ERROR", "", "\"" + task.content + "\"");
            countOfError++;

            observer.onError(task, msg);
        }

        private void onValidateComplete() {
            writeFooter();
            closeWriterIfNeeded(false);

            observer.onValidateComplete(outputFile);
        }
    }
}
