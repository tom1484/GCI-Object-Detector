package com.example.gciobjectdetector;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.widget.ImageView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Size previewSize = new Size(1080, 1440);

    private CameraPreview mCameraPreview;
    private ImageView mRecognitionView;

    private ObjectDetector mObjectdetector;
    private ObjectProcessor mObjectProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.CAMERA}, 34);
            while (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {}
        }

        mRecognitionView = (ImageView) findViewById(R.id.recognitionView);
        mCameraPreview = (CameraPreview) findViewById(R.id.cameraPreview);

        mObjectProcessor = new ObjectProcessor(this, previewSize);
        mObjectProcessor.setRecognitionView(mRecognitionView);

        try {
            mObjectdetector = new ObjectDetector(this, previewSize);
            mObjectdetector.setObjectProcessor(mObjectProcessor);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCameraPreview.setObjectDetector(mObjectdetector);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraPreview.onResume(this);
    }
    @Override
    protected void onPause() {
        mCameraPreview.onPause();
        super.onPause();
    }

}



