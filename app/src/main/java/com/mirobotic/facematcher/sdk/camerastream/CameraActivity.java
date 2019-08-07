/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.camerastream;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberlink.faceme.DetectionMode;
import com.cyberlink.facemedemo.camera.CameraController;
import com.cyberlink.facemedemo.camera.CameraFactory;
import com.cyberlink.facemedemo.camera.StatListener;
import com.cyberlink.facemedemo.sdk.FaceHolder;
import com.cyberlink.facemedemo.sdk.OnExtractedListener;
import com.cyberlink.facemedemo.ui.AutoFitTextureView;
import com.cyberlink.facemedemo.ui.BaseActivity;
import com.cyberlink.facemedemo.ui.BaseDialogFragment;
import com.cyberlink.facemedemo.ui.CLToast;
import com.cyberlink.facemedemo.ui.SettingsFragment;
import com.cyberlink.facemedemo.ui.UiSettings;
import com.mirobotic.facematcher.R;
import com.mirobotic.facematcher.sdk.imageviewer.ImageActivity;
import com.mirobotic.facematcher.sdk.manageface.ManageCollectionsActivity;
import com.mirobotic.facematcher.sdk.validate.ValidationActivity;
import com.mirobotic.facematcher.view.FaceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CameraActivity extends BaseActivity implements
        CameraController.Callback,
        FaceView.OnFaceClickListener,
        ResolutionFragment.Broker {
    private static final String TAG = "CameraActivity";

    private static final long AUTO_HIDE_INTERVAL = 2_000L;

    private Handler mainHandler = new Handler();
    private RecognitionHandler recognitionHandler;
    private UserInteractionHandler userInteractionHandler;
    private Integer forceRotateDegrees = null;

    private final ArrayList<View> viewsAutoHide = new ArrayList<>();
    private final ArrayList<View> viewsBottom = new ArrayList<>();
    private final ArrayList<View> viewsDebugTool = new ArrayList<>();

    private StatListener statListener;
    private CameraController cameraController;
    private TextView txtStatView;
    private FaceView facesView;
    private FaceAdapter faceAdapter;

    private abstract class BtnClickListener implements View.OnClickListener {
        @Override
        public final void onClick(View v) {
            autoHideViews();

            if (v.getAlpha() == 1F) {
                onClick();
            }
        }

        abstract void onClick();
    }

    @Override
    protected String getTagId() {
        return TAG;
    }

    @LayoutRes
    @Override
    protected int getContentLayout() {
        return R.layout.activity_camera;
    }

    @Override
    protected void initialize() {
        adjustFullscreenMode();
        // Initialize Camera controller and related components.
        initCameraController();
        // Initialize UI components
        initUiComponents();
        // Initialize UI default appearance.
        setDefaultUiAppearance();
    }

    @Override
    protected String[] getRequiredPermissions() {
        return new String[]{ Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE };
    }

    @Override
    protected String getPermissionString() {
        return "Camera and Storage";
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) adjustFullscreenMode();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        handleUiRotation(newConfig.orientation);
    }

    @UiThread
    private void handleUiRotation(int orientation) {
        cameraController.rotate();

        if (userInteractionHandler != null) userInteractionHandler.rotate();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateDebugToolsVisibility();
        mainHandler.postDelayed(updateStatRunnable, 1_000L);
        if (uiSettings.isShowInfo()) {
            showHideInfoPanel(true, false, 1_500L);
        } else {
            showHideInfoPanel(false, false, 0);
        }

        mainHandler.postDelayed(viewsAutoHideRunnable, AUTO_HIDE_INTERVAL);
        recognitionHandler.onResume();
        cameraController.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mainHandler.removeCallbacks(updateStatRunnable);
        recognitionHandler.onPause();
        cameraController.pause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cameraController.release();
        cameraController = null;
        recognitionHandler.onRelease();
        recognitionHandler = null;
    }

    private void adjustFullscreenMode() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    private void initCameraController() {
        AutoFitTextureView surfaceView = findViewById(R.id.cameraPreviewView);
        statListener = new StatListener();
        cameraController = CameraFactory.create(this, surfaceView, this, statListener);
        Size previewSize = uiSettings.getPreviewSize();
        cameraController.setResolution(previewSize.getWidth(), previewSize.getHeight());

        recognitionHandler = new RecognitionHandler(this, uiSettings, statListener, (width, height, faces) -> {
            facesView.updateFaceAttributes(width, height, faces);
            faceAdapter.replaceAll(faces);
        }, new OnExtractedListener() {
            @Override
            public void onExtracted(int width, int height, @NonNull List<FaceHolder> faces) {
                notifyUserActionDetectorIfNeeded(width, height, faces);
            }
        });
    }

    private final Runnable updateStatRunnable = new Runnable() {
        @Override
        public void run() {
            statListener.update();

            String statInfo
                    = "Frame:"
                    + "\n\tfps: " + format(statListener.getAverageFrameCaptured())
                    + "\nImage:"
                    + "\n\tfps: " + format(statListener.getAverageImageCaptured())
                    + "\nBitmap:"
                    + "\n\tfps: " + format(statListener.getAverageBitmapCreated())
                    + "\n\trotate(ms): " + format(statListener.getAverageBitmapRotatedTime())
                    + "\n\tcreate(ms): " + format(statListener.getAverageBitmapCreatedTime())
                    + "\nRecognizer:"
                    + "\n\tfps: " + format(statListener.getAverageFaceRecognized())
                    + "\n\ttotal(ms): " + format(statListener.getAverageFaceTotalTime())
                    + "\n\t\tdetectN(ms): " + format(statListener.getAverageNormalFaceDetectedTime());
            if (uiSettings.getDetectionMode() == DetectionMode.FAST) {
                statInfo += "\n\t\tdetectF(ms): " + format(statListener.getAverageFastFaceDetectedTime());
            }
            if (uiSettings.isShowFeatures()) {
                statInfo += "\n\t\textract(ms): " + format(statListener.getAverageFaceExtractedTime())
                        + "\n\t\trecognize(ms): " + format(statListener.getAverageFaceRecognizedTime());
            }

            txtStatView.setText(statInfo);

            mainHandler.postDelayed(this, 1_000L);
        }

        private String format(double value) {
            return String.format(Locale.US, "%.1f", value);
        }
    };

    private void initUiComponents() {
        initFaceView();
        initRotation();
        initResolution();
        initForceRotateButton();
        initCameraSwitch();
        initFacesButton();
        initInfoButton();
        initScanFolderButton();
        initValidationButton();
        initSetupButton();
        initUserActionButton();
        initAutoAddFaceButton();
        initManageFacesButton();
    }

    private void setDefaultUiAppearance() {
        showHideBottoms(false);
        findViewById(R.id.btnInfo).setSelected(uiSettings.isShowInfo());
        // XXX: Only show workaround for non brand manufacturer.
        List<String> manufacturer = Arrays.asList("asus", "dell", "google",
                "huawei", "htc", "lenovo", "lge", "meizu", "motorola", "oneplus", "oppo",
                "samsung", "sony", "vivo", "xiaomi", "zte");
        if (manufacturer.contains(Build.MANUFACTURER.toLowerCase())) {
            findViewById(R.id.btnRotate).setVisibility(View.GONE);
        }
    }

    private void initRotation() {
        View btnScreenRotation = findViewById(R.id.btnScreenRotation);
        btnScreenRotation.setOnClickListener(new BtnClickListener() {
            @Override
            void onClick() {
                boolean toLock = !btnScreenRotation.isSelected();
                btnScreenRotation.setSelected(toLock);

                int orientation = toLock
                        ? ActivityInfo.SCREEN_ORIENTATION_LOCKED
                        : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
                setRequestedOrientation(orientation);
            }
        });
        boolean isLocked = getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
        btnScreenRotation.setSelected(isLocked);

        viewsAutoHide.add(btnScreenRotation);
    }

    private void initResolution() {
        View btnResolution = findViewById(R.id.btnResolution);
        btnResolution.setOnClickListener(new BtnClickListener() {
            @Override
            void onClick() {
                btnResolution.setSelected(true);
                ResolutionFragment.newInstance().show(getSupportFragmentManager());
            }
        });
        viewsAutoHide.add(btnResolution);
    }

    private void initForceRotateButton() {
        ImageView btnRotate = findViewById(R.id.btnRotate);
        btnRotate.setOnClickListener(new BtnClickListener() {
            @Override
            void onClick() {
                if (forceRotateDegrees == null) {
                    forceRotateDegrees = 90;
                } else if (forceRotateDegrees == 90) {
                    forceRotateDegrees = 180;
                } else if (forceRotateDegrees == 180) {
                    forceRotateDegrees = 270;
                } else if (forceRotateDegrees == 270) {
                    forceRotateDegrees = 0;
                } else if (forceRotateDegrees == 0) {
                    forceRotateDegrees = null;
                }
                updateBtnRotateDrawableState(btnRotate);

                cameraController.forceBitmapRotation(forceRotateDegrees);
            }
        });

        forceRotateDegrees = cameraController.getForceBitmapRotation();
        updateBtnRotateDrawableState(btnRotate);

        viewsAutoHide.add(btnRotate);
    }

    private void updateBtnRotateDrawableState(ImageView btnRotate) {
        if (forceRotateDegrees == null) {
            btnRotate.setImageResource(R.drawable.ic_arrow_auto);
            btnRotate.setSelected(false);
        } else if (forceRotateDegrees == 90) {
            btnRotate.setImageResource(R.drawable.ic_arrow_90);
            btnRotate.setSelected(true);
        } else if (forceRotateDegrees == 180) {
            btnRotate.setImageResource(R.drawable.ic_arrow_180);
            btnRotate.setSelected(true);
        } else if (forceRotateDegrees == 270) {
            btnRotate.setImageResource(R.drawable.ic_arrow_270);
            btnRotate.setSelected(true);
        } else if (forceRotateDegrees == 0) {
            btnRotate.setImageResource(R.drawable.ic_arrow_0);
            btnRotate.setSelected(true);
        }
    }

    @Override
    public Size getCurrentResolution() {
        return uiSettings.getPreviewSize();
    }

    @Override
    public <S> List<S> getResolutions() {
        return cameraController.getResolutions();
    }

    @Override
    public void onResolutionChanged(Size size) {
        uiSettings.setPreviewSize(size.getWidth(), size.getHeight());
        cameraController.setResolution(size.getWidth(), size.getHeight());
    }

    private void initFaceView() {
        facesView = findViewById(R.id.faceView);
        facesView.setUiSettings(uiSettings);

        facesView.setOnFaceClickListener(this);
        findViewById(android.R.id.content).setOnClickListener((v) -> autoHideViews());
    }

    @Override
    public void onFaceClick(FaceHolder faceHolder) {
        if (faceHolder == null || !uiSettings.isShowFeatures()) {
            if (!uiSettings.isShowFeatures()) {
                CLToast.showLong(this, "Enable features extraction to recognize face.");
            }
            autoHideViews();
            return;
        }

        LayoutInflater factory = LayoutInflater.from(this);
        final View editView = factory.inflate(R.layout.view_edit_face, null);
        ImageView faceThumbnail = editView.findViewById(R.id.faceThumbnail);
        EditText editText = editView.findViewById(R.id.txtName);
        editText.setText(faceHolder.data.name, TextView.BufferType.EDITABLE);
        //Because it doesn't adjust bitmap size as square, not show face preview for V3.1.0
        if (faceHolder.faceBitmap != null) {
            faceThumbnail.setImageBitmap(faceHolder.faceBitmap);
        } else {
            faceThumbnail.setVisibility(View.GONE);
        }

        boolean forInsertion = TextUtils.isEmpty(faceHolder.data.name);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(forInsertion ? "Insert Face" : "Update Face")
                .setView(editView)
                .setCancelable(false)
                .setPositiveButton(forInsertion ? "Insert" : "Update", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!TextUtils.isEmpty(newName)) {
                        recognitionHandler.updateFace(faceHolder, newName);
                    }
                })
                .setNeutralButton("Cancel", null);
        if (!forInsertion) {
            builder.setNegativeButton("Remove", (dialog, which) -> recognitionHandler.removeFace(faceHolder));
        }
        builder.show();
    }

    private final Runnable viewsAutoHideRunnable = () -> {
        for (View view : viewsAutoHide) {
            if (view.isSelected()) continue;
            view.animate().alpha(0F).start();
        }
    };

    private void autoHideViews() {
        for (View view : viewsAutoHide) {
            if (view.isSelected()) continue;
            view.animate().alpha(1F).start();
        }

        delayAutoHideViews();
    }

    private void delayAutoHideViews() {
        mainHandler.removeCallbacks(viewsAutoHideRunnable);
        mainHandler.postDelayed(viewsAutoHideRunnable, AUTO_HIDE_INTERVAL);
    }

    @Override
    public void onSettingsChanged(boolean needRebuild) {
        recognitionHandler.applySettings(needRebuild);
    }

    @Override
    public <F extends BaseDialogFragment> void onFragmentDetach(F fragment) {
        if (fragment instanceof SettingsFragment) {
            findViewById(R.id.btnSettings).setSelected(false);
            delayAutoHideViews();
        } else if (fragment instanceof ResolutionFragment) {
            findViewById(R.id.btnResolution).setSelected(false);
            delayAutoHideViews();
        }
    }

    private void initCameraSwitch() {
        ImageSwitcher btnSwitchCamera = findViewById(R.id.btnCamera);
        if (cameraController.getUiLogicalCameraNum() > 1) {
            BtnClickListener listener = new BtnClickListener() {
                @Override
                void onClick() {
                    boolean isRear = cameraController.switchCamera();
                    btnSwitchCamera.setDisplayedChild(isRear ? 1 : 0);
                }
            };
            btnSwitchCamera.setOnClickListener(listener);
            viewsAutoHide.add(btnSwitchCamera);
        } else {
            btnSwitchCamera.setVisibility(View.GONE);
        }
    }

    private void initInfoButton() {
        View btnInfo = findViewById(R.id.btnInfo);

        View.OnClickListener listener = new BtnClickListener() {
            @Override
            void onClick() {
                if (uiSettings.isShowInfo()) {
                    uiSettings.setShowInfo(false);
                } else {
                    uiSettings.setShowInfo(true);
                }
                btnInfo.setSelected(uiSettings.isShowInfo());
                showHideInfoPanel(uiSettings.isShowInfo(), true, 0);
            }
        };

        txtStatView = findViewById(R.id.txtStat);
        txtStatView.setOnClickListener(listener);

        btnInfo.setOnClickListener(listener);
        viewsAutoHide.add(btnInfo);
        viewsBottom.add(btnInfo);
    }

    private void initScanFolderButton() {
        View btnScanFolder = findViewById(R.id.btnScanFolder);
        btnScanFolder.setOnClickListener(new BtnClickListener() {
            @Override
            void onClick() {
                Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
                startActivity(intent);
            }
        });
        viewsAutoHide.add(btnScanFolder);
        viewsBottom.add(btnScanFolder);
        viewsDebugTool.add(btnScanFolder);
    }

    private void initValidationButton() {
        View btnValidation = findViewById(R.id.btnValidation);
        btnValidation.setOnClickListener(new BtnClickListener() {
            @Override
            void onClick() {
                Intent intent = new Intent(getApplicationContext(), ValidationActivity.class);
                startActivity(intent);
            }
        });
        viewsAutoHide.add(btnValidation);
        viewsBottom.add(btnValidation);
        viewsDebugTool.add(btnValidation);
    }

    private void showHideInfoPanel(boolean show, boolean animate, long startDelay) {
        txtStatView.animate()
                .setStartDelay(startDelay)
                .alpha(show ? 1 : 0)
                .translationX(show ? 0 : -txtStatView.getWidth())
                .setDuration(animate ? 300 : 0)
                .start();
    }

    private void initSetupButton() {
        View btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(new BtnClickListener() {
            @Override
            void onClick() {
                btnSettings.setSelected(true);
                SettingsFragment.newInstance(true, true).show(getSupportFragmentManager());
            }
        });
        viewsAutoHide.add(btnSettings);
        viewsBottom.add(btnSettings);
    }

    private void initUserActionButton() {
        View btnUserAction = findViewById(R.id.btnUserAction);
        btnUserAction.setOnClickListener(new BtnClickListener() {
            @Override
            void onClick() {
                if (!uiSettings.isShowFeatures()) {
                    CLToast.showLong(CameraActivity.this, "Enable features extraction to do user interaction.");
                    return;
                }

                boolean enableUserInteraction = !btnUserAction.isSelected();
                btnUserAction.setSelected(enableUserInteraction);

                if (enableUserInteraction) {
                    startUserInteraction();
                } else {
                    releaseUserActionDetector();
                }
            }
        });
        viewsAutoHide.add(btnUserAction);
        viewsBottom.add(btnUserAction);
    }

    private void initAutoAddFaceButton() {
        View btnAutoAddFace = findViewById(R.id.btnAutoAddFace);
        btnAutoAddFace.setOnClickListener(new BtnClickListener() {
            @Override
            void onClick() {
                boolean enableAutoAddFace = !btnAutoAddFace.isSelected();
                btnAutoAddFace.setSelected(enableAutoAddFace);

                recognitionHandler.enableAutoAddFace(enableAutoAddFace);
            }
        });
        viewsAutoHide.add(btnAutoAddFace);
        viewsBottom.add(btnAutoAddFace);
        viewsDebugTool.add(btnAutoAddFace);
    }

    private void startUserInteraction() {
        releaseUserActionDetector();

        userInteractionHandler = new UserInteractionHandler(this,
                findViewById(R.id.layoutUserInteraction),
                findViewById(R.id.layoutUserInteractionMask),
                () -> {
                    autoHideViews();
                    findViewById(R.id.btnUserAction).setSelected(false);
                });
    }

    private void notifyUserActionDetectorIfNeeded(int width, int height, List<FaceHolder> faces) {
        if (userInteractionHandler != null) {
            if (!uiSettings.isShowFeatures()) {
                CLToast.showLong(CameraActivity.this, "Enable features extraction to do user interaction.");
                return;
            }

            userInteractionHandler.detect(recognitionHandler.getConfidenceThreshold(), width, height, faces);
        }
    }

    private void releaseUserActionDetector() {
        if (userInteractionHandler != null) {
            userInteractionHandler.release();
            userInteractionHandler = null;
        }
    }

    private void initManageFacesButton() {
        View btnManageFaces = findViewById(R.id.btManageFaces);
        btnManageFaces.setOnClickListener(new BtnClickListener() {
            @Override
            void onClick() {
                Intent intent = new Intent(getApplicationContext(), ManageCollectionsActivity.class);
                startActivity(intent);
            }
        });
        viewsAutoHide.add(btnManageFaces);
        viewsBottom.add(btnManageFaces);
    }

    private void updateDebugToolsVisibility() {
        int visibility = UiSettings.isDebugMode() ? View.VISIBLE : View.GONE;
        for (View debugView : viewsDebugTool) {
            debugView.setVisibility(visibility);
        }
    }

    private void initFacesButton() {
        initFaceAdapter();

        View btnFaces = findViewById(R.id.btnFaces);
        btnFaces.setOnClickListener(new BtnClickListener() {
            @Override
            void onClick() {
                boolean toShow = !btnFaces.isSelected();
                btnFaces.setSelected(toShow);

                autoHideViews();
                showHideBottoms(toShow);
            }
        });

        viewsAutoHide.add(btnFaces);
        viewsBottom.add(btnFaces);
    }

    private void showHideBottoms(boolean toShow) {
        int translateY = toShow ? 0 : getResources().getDimensionPixelSize(R.dimen.face_list_width);

        for (View view : viewsBottom) {
            view.animate().translationY(translateY).start();
        }
    }

    private void initFaceAdapter() {
        faceAdapter = new FaceAdapter(this, uiSettings);

        RecyclerView facesListView = findViewById(R.id.faceList);
        facesListView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        facesListView.setAdapter(faceAdapter);
        viewsBottom.add(facesListView);
    }

    @Override
    public void onBitmap(Bitmap bitmap) {
        RecognitionHandler recognizer = recognitionHandler;
        if (recognizer != null) recognizer.onBitmap(bitmap);
    }

    @Override
    public void checkTask(CameraController.CheckCallback callback) {
        RecognitionHandler recognizer = recognitionHandler;
        if (recognizer != null && !recognizer.isRecognizing()) {
            callback.acquired();
        } else {
            callback.rejected();
        }
    }
}
