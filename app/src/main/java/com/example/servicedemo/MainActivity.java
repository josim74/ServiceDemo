package com.example.servicedemo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    public static final String PROGRESS_UPDATE = "progress_update";
    private static final int PERMISSION_REQUEST_CODE = 1;
    ImageView imageView;
    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermission()) {
                    startImageDownload();
                } else {
                    requestPermission();
                }
            }
        });

        registerReceiver();
    }

    private void registerReceiver() {

        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PROGRESS_UPDATE);
        bManager.registerReceiver(mBroadcastReceiver, intentFilter);

    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(PROGRESS_UPDATE)) {

                String downloadComplete = intent.getStringExtra("downloadComplete");
                //Log.d("API123", download.getProgress() + " current progress");

                if (downloadComplete != null) {

                    Toast.makeText(getApplicationContext(), "File download completed", Toast.LENGTH_SHORT).show();

                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator +
                            downloadComplete);

                    Picasso.get().load(file).into(imageView);

                }
            }
        }
    };


    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void startImageDownload() {

        Log.d("TRACKING", "startImageDownload: 1");
        Intent intent = new Intent(this, MyService.class);
        startService(intent);

    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    startImageDownload();
                } else {

                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();

                }
                break;
        }
    }
}

