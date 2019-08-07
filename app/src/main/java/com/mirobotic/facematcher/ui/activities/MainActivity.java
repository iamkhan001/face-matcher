package com.mirobotic.facematcher.ui.activities;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cyberlink.faceme.DetectionModelSpeedLevel;
import com.cyberlink.faceme.EnginePreference;
import com.cyberlink.faceme.ExtractConfig;
import com.cyberlink.faceme.ExtractionModelSpeedLevel;
import com.cyberlink.faceme.FaceAttribute;
import com.cyberlink.faceme.FaceInfo;
import com.cyberlink.faceme.FaceMeRecognizer;
import com.cyberlink.faceme.LicenseManager;
import com.cyberlink.facemedemo.camera.CameraController;
import com.cyberlink.facemedemo.camera.CameraFactory;
import com.cyberlink.facemedemo.camera.StatListener;
import com.cyberlink.facemedemo.sdk.FaceHolder;
import com.cyberlink.facemedemo.sdk.OnExtractedListener;
import com.cyberlink.facemedemo.ui.AutoFitTextureView;
import com.mirobotic.facematcher.R;
import com.mirobotic.facematcher.app.Config;
import com.mirobotic.facematcher.ui.fragments.CameraFragment;
import com.mirobotic.facematcher.view.FaceView;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnImageCaptureListener{

    private Context context;
    private boolean isRegistered;
    private int result;
    private LicenseManager licenseManager;
    private final String tag = MainActivity.class.getSimpleName();
    private FaceMeRecognizer recognizer;
    private ExtractConfig extractConfig;
    private boolean isReady = true;
    private StatListener statListener;
    private CameraController cameraController;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = MainActivity.this;

        registerLicense();

        if (null == savedInstanceState && isRegistered) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentView, CameraFragment.newInstance())
                    .commit();
        }

    }

    private void registerLicense() {
        licenseManager = new LicenseManager(context);
        isRegistered = licenseManager.initialize(Config.API_KEY);
        result = licenseManager.registerLicense();
        Log.e(tag,"Result "+isRegistered+" >> "+result);

        if (result!=0){
            Toast.makeText(context,"Registration Failed!",Toast.LENGTH_SHORT).show();
            return;
        }

        recognizer = new FaceMeRecognizer(context);
        recognizer.initialize(
                EnginePreference.PREFER_NONE,
                2,
                DetectionModelSpeedLevel.DEFAULT,
                ExtractionModelSpeedLevel.HIGH,
                Config.API_KEY
        );

        extractConfig = new ExtractConfig();
        extractConfig.extractFeature = true;
        extractConfig.extractAge = true;
        extractConfig.extractEmotion = true;
        extractConfig.extractGender = true;



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        licenseManager.registerLicense();
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    @Override
    public void onRecogniseFace(Bitmap bitmap) {

        if (!isReady){
            return;
        }

        isReady = false;


        if (bitmap==null){
            Log.e(tag,"BITMAP IS NULLLLLLLLLLL");
        }

        new Thread(() -> {
            int faceCount = recognizer.extractFace(extractConfig,Collections.singletonList(bitmap));

            Log.d(tag,"CAPTURE FACE COUNT >> "+faceCount);

            for (int index=0; index<faceCount;index++){
                FaceInfo faceInfo = recognizer.getFaceInfo(0,index);
                FaceAttribute faceAttribute = recognizer.getFaceAttribute(0,index);

                Log.e(tag,"RESULT confidence "+faceInfo.confidence+
                        "\nage "+faceAttribute.age+
                        "\ngender "+faceAttribute.gender+
                        "\nemotion "+faceAttribute.emotion
                );
            }

            isReady = true;
        }).start();

    }


}
