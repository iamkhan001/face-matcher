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
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
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

import com.cyberlink.faceme.DetectionMode;
import com.cyberlink.facemedemo.camera.CameraController;
import com.cyberlink.facemedemo.camera.CameraFactory;
import com.cyberlink.facemedemo.camera.StatListener;
import com.cyberlink.facemedemo.sdk.FaceHolder;
import com.cyberlink.facemedemo.sdk.OnExtractedListener;
import com.cyberlink.facemedemo.ui.AutoFitTextureView;
import com.cyberlink.facemedemo.ui.BaseActivity;
import com.cyberlink.facemedemo.ui.CLToast;
import com.mirobotic.facematcher.R;
import com.mirobotic.facematcher.view.FaceView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraActivity extends BaseActivity implements
        CameraController.Callback,
        FaceView.OnFaceClickListener,
        ResolutionFragment.Broker {
    private static final String TAG = "CameraActivity";


    private Handler mainHandler = new Handler();
    private RecognitionHandler recognitionHandler;


    private StatListener statListener;
    private CameraController cameraController;
    private FaceView facesView;

    private ArrayList<String> savedFaces;


    private abstract class BtnClickListener implements View.OnClickListener {
        @Override
        public final void onClick(View v) {

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

        getSavedFaces(getApplicationContext());

        Log.e(TAG,"saved faces >> "+savedFaces.size());

        adjustFullscreenMode();
        // Initialize Camera controller and related components.
        initCameraController();
        // Initialize UI components
        initUiComponents();
        // Initialize UI default appearance.
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

    }

    @Override
    protected void onResume() {
        super.onResume();


        recognitionHandler.onResume();
        cameraController.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        }, new OnExtractedListener() {
            @Override
            public void onExtracted(int width, int height, @NonNull List<FaceHolder> faces) {

                Log.e(TAG,"face count >> "+faces.size());

                for (FaceHolder face:faces){
                    Log.e(TAG,face.data.name+" "+face.data.faceId);




                }


            }
        });
    }



    private void initUiComponents() {
        initFaceView();
        initCameraSwitch();
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
    }

    @Override
    public void onFaceClick(FaceHolder faceHolder) {
//        if (true) {
        if (faceHolder == null || !uiSettings.isShowFeatures()) {
            if (!uiSettings.isShowFeatures()) {
                CLToast.showLong(this, "Enable features extraction to recognize face.");
            }
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
//                        recognitionHandler.updateFace(faceHolder, newName);
                        if (faceHolder.faceBitmap!=null){
                            Uri uri = saveImageToInternalStorage(getApplicationContext(),faceHolder.faceBitmap,newName);
                            Log.e(TAG,"saved to "+uri.getPath());
                        }
                    }
                })
                .setNeutralButton("Cancel", null);
        if (!forInsertion) {
            builder.setNegativeButton("Remove", (dialog, which) -> recognitionHandler.removeFace(faceHolder));
        }
        builder.show();
    }


    public static Uri saveImageToInternalStorage(Context mContext, Bitmap bitmap, String name){


        String mImageName = "face_"+name+".jpg";

        ContextWrapper wrapper = new ContextWrapper(mContext);

        File file = wrapper.getDir("School",MODE_PRIVATE);

        file = new File(file, mImageName);

        try{

            OutputStream stream = null;

            stream = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);

            stream.flush();

            stream.close();

        }catch (IOException e)
        {
            e.printStackTrace();
        }

        return Uri.parse(file.getAbsolutePath());
    }

    public void getSavedFaces(Context context) {

        File[] listFile;

        savedFaces = new ArrayList<>();

        ContextWrapper wrapper = new ContextWrapper(context);

        File file = wrapper.getDir("School",MODE_PRIVATE);

        if (file.isDirectory())
        {
            listFile = file.listFiles();

            if (listFile==null){
                return;
            }

            for (File file1 : listFile) {
                savedFaces.add(file1.getAbsolutePath());
            }
        }
    }



    @Override
    public void onSettingsChanged(boolean needRebuild) {
        recognitionHandler.applySettings(needRebuild);
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
        } else {
            btnSwitchCamera.setVisibility(View.GONE);
        }
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
