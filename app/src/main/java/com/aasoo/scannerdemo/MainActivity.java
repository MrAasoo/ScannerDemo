package com.aasoo.scannerdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.aasoo.scannerdemo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> cameraScannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonGoogleScanner.setOnClickListener(this);
        binding.buttonCameraScanner.setOnClickListener(this);

        cameraScannerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult o) {
                Intent intent = o.getData();
                if (o.getResultCode() == RESULT_OK) {
                    binding.setScanValue(intent.getStringExtra(CameraScannerActivity.SCAN_VALUE));
                } else {
                    Toast.makeText(MainActivity.this, intent.getStringExtra(CameraScannerActivity.ERROR), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public void onClick(View v) {
        if (v == binding.buttonCameraScanner) {
            Intent cameraScanner = new Intent(this, CameraScannerActivity.class);
            cameraScannerLauncher.launch(cameraScanner);
        } else if (v == binding.buttonGoogleScanner) {
        }
    }
}