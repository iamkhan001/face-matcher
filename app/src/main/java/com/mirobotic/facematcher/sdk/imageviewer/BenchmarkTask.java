/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.imageviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.cyberlink.faceme.DetectionMode;
import com.cyberlink.faceme.DetectionSpeedLevel;
import com.cyberlink.faceme.EnginePreference;
import com.cyberlink.faceme.ExtractionModelSpeedLevel;
import com.cyberlink.faceme.FaceFeature;
import com.cyberlink.faceme.FaceInfo;
import com.cyberlink.facemedemo.sdk.FaceMeRecognizerWrapper;
import com.cyberlink.facemedemo.ui.UiSettings;
import com.mirobotic.facematcher.utils.Average;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class BenchmarkTask extends Thread {
    private static final String TAG = "BenchmarkTask";

    interface Observer {
        default void onBenchmarkBegin() {}

        default void onItemBegin(ImageItem imageItem) {}

        default void onBitmapLoaded(ImageItem imageItem) {}
        default void onBitmapFailed(ImageItem imageItem) {}

        default void onFaceFailed(ImageItem imageItem) {}
        default void onFaceRecognized(ImageItem imageItem, List<FaceInfo> faces) {}

        default void onItemEnd(ImageItem imageItem, long duration) {}

        void onBenchmarkProgress(int progress); // from 0 ~ 100.
        default void onBenchmarkFailed(String message) {}
        void onBenchmarkEnd(File outputFile, double avgTotal, double avgNormalDetect, double avgFastDetect, double avgExtract);
    }

    private final Context context;
    private final List<ImageItem> imageItems;
    private final Reporter reporter;
    private final UiSettings uiSettings;
    private final File benchmarkFolder;

    private transient boolean isCancelled = false;
    private FaceMeRecognizerWrapper faceMeRecognizer;

    BenchmarkTask(Context context, List<ImageItem> imageItems, UiSettings uiSettings, Observer observer) {
        super(TAG);

        this.context = context.getApplicationContext();
        this.imageItems = imageItems;
        this.reporter = new Reporter(imageItems.size(), observer);
        this.uiSettings = uiSettings;
        this.benchmarkFolder = uiSettings.getBenchmarkFolder();
    }

    void cancel() {
        isCancelled = true;
        this.interrupt();
    }

    @Override
    public void run() {
        Process.setThreadPriority(-10 /* higher than display */);
        long start = System.currentTimeMillis();

        if (isCancelled) return;
        reporter.onBenchmarkProgress(1); // XXX: Task start executing.
        reporter.onBenchmarkBegin();

        try {
            faceMeRecognizer = new FaceMeRecognizerWrapper(context);

            // 1st: LicenseManager.
            faceMeRecognizer.registerLicense();

            // 2nd: FaceMeRecognizer.
            faceMeRecognizer.initialize();
        } catch (Exception e) {
            Log.e(TAG, "Cannot setup FaceMe components", e);
            if (faceMeRecognizer != null) faceMeRecognizer.release();
            reporter.onBenchmarkFailed("Cannot setup FaceMe components\n" + e.getMessage());
            return;
        }

        faceMeRecognizer.configure();

        if (isCancelled) {
            faceMeRecognizer.release();
            return;
        }
        reporter.onBenchmarkProgress(2); // XXX: Face Recognition Engine loaded.

        recognizeFiles();

        faceMeRecognizer.release();

        if (isCancelled) {
            reporter.closeWriterIfNeeded(true);
            return;
        }

        reporter.onBenchmarkProgress(100); // XXX: Task finished.
        reporter.onBenchmarkEnd();
    }

    private void recognizeFiles() {
        for (ImageItem imageItem : imageItems) {
            if (isCancelled) return;

            recognizeFile(imageItem);
        }
    }

    private void recognizeFile(ImageItem imageItem) {
        long start = System.currentTimeMillis();
        reporter.onItemBegin(imageItem);

        Bitmap bitmap = decodeBitmap(imageItem);
        if (bitmap == null) return;

        int facesCount = recognizeFaces(imageItem, bitmap);
        if (facesCount < 0) return;

        bitmap.recycle();
        reporter.onItemEnd(imageItem, System.currentTimeMillis() - start);
    }

    private Bitmap decodeBitmap(ImageItem imageItem) {
        long start = System.currentTimeMillis();

        Bitmap bitmap = null;
        Size resolution = new Size(0, 0);
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            bitmap = BitmapFactory.decodeFile(imageItem.file.getAbsolutePath(), options);
            // Handle EXIF orientation.
            if ("image/jpeg".equals(options.outMimeType)) {
                bitmap = rotateJpegBitmapIfNeeded(imageItem, bitmap);
            }
            resolution = new Size(bitmap.getWidth(), bitmap.getHeight());
        } catch (OutOfMemoryError oome) {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageItem.file.getAbsolutePath(), options);
            resolution = new Size(options.outWidth, options.outHeight);
            Log.w(TAG, "Image is too large to decode: " + resolution);
        } catch (Throwable t) {
            Log.e(TAG, "Cannot decode Bitmap", t);
        }

        if (isCancelled) {
            if (bitmap != null) bitmap.recycle();
            return null;
        }

        imageItem.setResolution(resolution);
        if (bitmap == null) {
            reporter.onBitmapFailed(imageItem, resolution);
        } else {
            reporter.onBitmapLoaded(imageItem, System.currentTimeMillis() - start, resolution);
        }
        return bitmap;
    }

    private Bitmap rotateJpegBitmapIfNeeded(ImageItem imageItem, Bitmap bitmap) throws IOException {
        ExifInterface exifInterface = new ExifInterface(imageItem.file.getAbsolutePath());
        int degrees = exifInterface.getRotationDegrees();
        if (degrees == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();

        return rotatedBitmap;
    }

    private int recognizeFaces(ImageItem imageItem, Bitmap bitmap) {
        int facesCount = faceMeRecognizer.extractFace(bitmap);
        if (isCancelled) {
            bitmap.recycle();
            return -1;
        }
        //noinspection unchecked
        Map<String, Integer> profileStatus = (Map) faceMeRecognizer.getProperty("Performance");

        List<FaceInfo> faces = new ArrayList<>();
        if (facesCount > 0) {
            for (int idx = 0; idx < facesCount; idx++) {
                faces.add(faceMeRecognizer.getFaceInfo(idx));
            }
        }

        reporter.onFaceRecognized(imageItem, faces, profileStatus);
        return facesCount;
    }

    private class Reporter {
        private static final int PROGRESS_SCALE = 3; // Bitmap + Recognize + compareFeatureVector
        private static final char COLUMN_SEPARATOR = ',';

        private static final int NUMBER_OF_CACHE_FACE_FEATURE = 100;
        private static final boolean OUTPUT_FACE_BOUNDING_BOX = true;
        private static final boolean COMPARE_FEATURE_VECTOR = true;

        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        private final Average avgBitmapDuration = new Average();
        private final Average avgTotalDuration = new Average();
        private final Average avgNormalDetectDuration = new Average();
        private final Average avgFastDetectDuration = new Average();
        private final Average avgExtractDuration = new Average();

        private final List<FaceFeature> faceFeatures = new ArrayList<>();

        private final float maxProgress;
        private final Observer observer;

        private File outputFile;
        private Writer logWriter = null;
        private int itemIndex = 0;
        private int lastProgress = 0;

        Reporter(int size, Observer observer) {
            this.maxProgress = size * PROGRESS_SCALE / 100F;
            this.observer = observer;
        }

        private void createWriterIfNeeded() {
            if (logWriter != null) return;

            SimpleDateFormat filenameFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
            outputFile = new File(benchmarkFolder, "FaceMe_" + filenameFormat.format(new Date()) + ".csv");
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
            writeLine("Path", benchmarkFolder.getAbsolutePath());
            writeLine("Device", Build.VERSION.RELEASE, Build.DEVICE, Build.MODEL, Build.HARDWARE, Build.BRAND);
            writeLine("Time", new Date());
            writeLine("SDK Version", com.cyberlink.faceme.BuildConfig.VERSION_NAME);
            writeLine();
            writeLine("minFaceWidth", uiSettings.getMinFaceWidth());
            writeLine("EngineThreads", uiSettings.getEngineThreads());
            writeLine("DetectMethod", getDetectMethodString(uiSettings.getEnginePreference()));
            writeLine("ExtractModel", getExtractModelString(uiSettings.getExtractModel()));
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
            if (COMPARE_FEATURE_VECTOR) writeLine("", "", "", "", "", "", "", "", "", "", "compareFeatureVector(times)");
            write("File", "Bitmap(ms)", "width", "height", "Face#", "Total(ms)", "NormalDetect", "FastDetect", "Extract");
            if (COMPARE_FEATURE_VECTOR) write("2000#");
            if (OUTPUT_FACE_BOUNDING_BOX) write("BoundingBox");

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
            write("Average",
                    String.format(Locale.US, "%.1f", avgBitmapDuration.get()),
                    "", "", "",
                    String.format(Locale.US, "%.1f", avgTotalDuration.get()),
                    String.format(Locale.US, "%.1f", avgNormalDetectDuration.get()),
                    String.format(Locale.US, "%.1f", avgFastDetectDuration.get()),
                    String.format(Locale.US, "%.1f", avgExtractDuration.get())
            );
            writeLine();
        }

        private void onBenchmarkBegin() {
            writeHeader();
            mainHandler.post(observer::onBenchmarkBegin);
        }

        private void onItemBegin(ImageItem imageItem) {
            reportItemProgress(0);

            String filename = imageItem.file.getAbsolutePath().substring(benchmarkFolder.getAbsolutePath().length() + 1);
            write(filename);
            mainHandler.post(() -> observer.onItemBegin(imageItem));
        }

        private void onBitmapLoaded(ImageItem imageItem, long duration, Size resolution) {
            reportItemProgress(1);

            avgBitmapDuration.add(duration);
            write(duration, resolution.getWidth(), resolution.getHeight());
            mainHandler.post(() -> observer.onBitmapLoaded(imageItem));
        }

        private void onBitmapFailed(ImageItem imageItem, Size resolution) {
            reportItemProgress(PROGRESS_SCALE);
            itemIndex++;

            writeLine("", resolution.getWidth(), resolution.getHeight());
            mainHandler.post(() -> observer.onBitmapFailed(imageItem));
        }

        private void onFaceRecognized(ImageItem imageItem, List<FaceInfo> faces, @Nullable Map<String, Integer> profileStatus) {
            reportItemProgress(2);

            int facesCount = faces.size();
            write(facesCount);

            if (profileStatus != null) {
                int total = profileStatus.containsKey("Total") ? profileStatus.get("Total") : 0;
                avgTotalDuration.add(total);
                write(total);

                int normalDetect = profileStatus.containsKey("FaceDetect") ? profileStatus.get("FaceDetect") : 0;
                if (normalDetect > 0) {
                    avgNormalDetectDuration.add(normalDetect);
                    write(normalDetect);
                } else {
                    write("");
                }

                int fastDetect = profileStatus.containsKey("FastFaceDetect") ? profileStatus.get("FastFaceDetect") : 0;
                if (fastDetect > 0) {
                    avgFastDetectDuration.add(fastDetect);
                    write(fastDetect);
                } else {
                    write("");
                }

                if (facesCount > 0) {
                    int extract = profileStatus.containsKey("Extract") ? profileStatus.get("Extract") : 0;
                    avgExtractDuration.add(extract);

                    write(extract);

                    if (COMPARE_FEATURE_VECTOR) compareFeatureData(faces.size());
                    if (OUTPUT_FACE_BOUNDING_BOX) write(composeBoundingBox(faces));
                }
            }

            reportItemProgress(3);
            mainHandler.post(() -> observer.onFaceRecognized(imageItem, faces));
        }

        private void compareFeatureData(int facesCount) {
            if (facesCount <= 0) return;

            List<FaceFeature> thisFeatures = new ArrayList<>();
            for (int idx = 0; idx < facesCount; idx++) {
                FaceFeature faceFeature = faceMeRecognizer.getFaceFeature(idx);
                if (faceFeature != null) {
                    thisFeatures.add(faceFeature);
                    faceFeatures.add(faceFeature);
                    if (faceFeatures.size() > NUMBER_OF_CACHE_FACE_FEATURE) faceFeatures.remove(0);
                }
            }
            if (thisFeatures.isEmpty()) return;

            int imageRandomFace = (int) (Math.random() * (thisFeatures.size() - 1));
            FaceFeature faceA = thisFeatures.get(imageRandomFace);

            int cacheRandomFace = (int) (Math.random() * (faceFeatures.size() - 1));
            FaceFeature faceB = faceFeatures.get(cacheRandomFace);

            //massCompareFeatureData(faceA, faceB, 1000);
            massCompareFeatureData(faceA, faceB, 2000);
            //massCompareFeatureData(faceA, faceB, 5000);
            //massCompareFeatureData(faceA, faceB, 10000);
        }

        private void massCompareFeatureData(FaceFeature faceA, FaceFeature faceB, int loopCount) {
            List params = Arrays.asList(faceA, faceB, loopCount);
            long start = System.currentTimeMillis();
            boolean success = faceMeRecognizer.setProperty("AndroidLoopCompareVector", params);
            long duration = System.currentTimeMillis() - start;

            write(success ? duration : "#ERROR");
        }

        private String composeBoundingBox(List<FaceInfo> faces) {
            StringBuilder sb = new StringBuilder();
            if (!faces.isEmpty()) {
                sb.append("\"");
                for (int idx = 0; idx < faces.size(); idx++) {
                    FaceInfo faceInfo = faces.get(idx);
                    sb.append("[")
                            .append(faceInfo.boundingBox.left).append(",")
                            .append(faceInfo.boundingBox.top).append(",")
                            .append(faceInfo.boundingBox.right).append(",")
                            .append(faceInfo.boundingBox.bottom).append("]");
                    if (idx != (faces.size() - 1)) {
                        sb.append(",");
                    }
                }
                sb.append("\"");
            }
            return sb.toString();
        }

        private void onItemEnd(ImageItem imageItem, long duration) {
            reportItemProgress(PROGRESS_SCALE);
            itemIndex++;

            writeLine();
            mainHandler.post(() -> observer.onItemEnd(imageItem, duration));
        }

        private void onBenchmarkProgress(int progress) {
            if (lastProgress >= progress) return;
            lastProgress = progress;

            mainHandler.post(() -> observer.onBenchmarkProgress(progress));
        }

        private void onBenchmarkFailed(String message) {
            BenchmarkTask.this.cancel();
            mainHandler.post(() -> observer.onBenchmarkFailed(message));
        }

        private void onBenchmarkEnd() {
            writeFooter();
            closeWriterIfNeeded(false);
            mainHandler.post(() -> observer.onBenchmarkEnd(outputFile, avgTotalDuration.get(),
                    avgNormalDetectDuration.get(), avgFastDetectDuration.get(), avgExtractDuration.get()));
        }

        private void reportItemProgress(int stage /* 0 ~ 2 */) {
            int progress = (int) Math.floor(((itemIndex * PROGRESS_SCALE) + stage) / maxProgress);
            onBenchmarkProgress(progress);
        }
    }
}
