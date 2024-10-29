package com.aasoo.scannerdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.aasoo.scannerdemo.databinding.ActivityCameraScannerBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class CameraScannerActivity extends AppCompatActivity {

    public static final String SCAN_VALUE = "scan_value";
    public static final String ERROR = "error";
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private final BarcodeScanner barcodeScanner = BarcodeScanning.getClient();
    private ActivityCameraScannerBinding binding;

    // ActivityResultLauncher for requesting permission
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera(); // Start camera if permission is granted
                } else {
                    // Handle the case when permission is denied
                    Log.e("ScannerActivity", "Camera permission denied");
                    setScanResult(RESULT_CANCELED, "", "Camera permission denied");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestCameraPermission();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setScanResult(RESULT_CANCELED, "", "Scan canceled");
                setEnabled(false);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProviderFuture != null)
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera(); // Permission is already granted
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    // Show explanation why camera is needed
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.camera_permission_needed)
                            .setMessage(R.string.app_needs_camera_permission)
                            .setPositiveButton("OK", (dialog, which) -> requestPermissionLauncher.launch(Manifest.permission.CAMERA))
                            .setNegativeButton("Cancel", (dialog, which) -> setScanResult(RESULT_CANCELED, "", "Camera permission denied"))
                            .show();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA); // Request permission
                }
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA); // Request permission
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreviewAndAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraScannerActivity", "Error starting camera", e);
                setScanResult(RESULT_CANCELED, "", "Error starting camera");
            } catch (Exception e) {
                Log.e("CameraScannerActivity", "Unexpected error during camera start", e);
                setScanResult(RESULT_CANCELED, "", "Unexpected error during camera start");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreviewAndAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new BarcodeAnalyzer());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private class BarcodeAnalyzer implements ImageAnalysis.Analyzer {

        @SuppressLint("UnsafeOptInUsageError")
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            if (imageProxy.getImage() == null) {
                imageProxy.close();
                return;
            }

            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees());

            barcodeScanner.process(image)
                    .addOnSuccessListener(this::handleBarcodes)
                    .addOnFailureListener(e -> Log.e("ScannerActivity", "Barcode analysis failed", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        }

        private void handleBarcodes(List<Barcode> barcodes) {
            for (Barcode barcode : barcodes) {
                Log.d("ScannerActivity", "Barcode Format: " + barcode.getFormat());
                Log.d("ScannerActivity", "Barcode Value Type: " + barcode.getValueType());
                Log.d("ScannerActivity", "Barcode Detected: " + barcode.getRawValue());
                setScanResult(RESULT_OK, barcode.getRawValue(), "");
            }
        }
    }

    private void setScanResult(int RESULT, String rawValue, String error) {
        Intent intent = new Intent();
        intent.putExtra(SCAN_VALUE, rawValue);
        intent.putExtra(ERROR, error);
        setResult(RESULT, intent);
        finish();
    }
}