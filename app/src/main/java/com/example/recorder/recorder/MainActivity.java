package com.example.recorder.recorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.recorder.recorder.recorders.IRecorder;
import com.example.recorder.recorder.recorders.impl.RecorderAudio;
import com.example.recorder.recorder.recorders.impl.RecorderMedia;
import com.example.recorder.recorder.view.wave.WaveView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final int CODE = 1;

    @BindView(R.id.btn_start)
    public Button btnStart;
    @BindView(R.id.btn_stop)
    public Button btnStop;
    @BindView(R.id.v_wave)
    public WaveView waveView;

    private IRecorder recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        List<String> permissionList = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }

        if (permissionList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissions, CODE);
        }
        recorder = new RecorderMedia();
//        recorder = new RecorderAudio();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean hasRefused = false;
        if (requestCode == CODE) {
            for (int grentResult : grantResults) {
                if (grentResult == -1) {
                    hasRefused = true;
                    break;
                }

            }
        }

        if (hasRefused) {
            finish();
        }
    }

    @OnClick({R.id.btn_stop, R.id.btn_start})
    public void onClickListener(View v) {
        switch (v.getId()) {
            case R.id.btn_stop:
                recorder.stopRecorde();
                break;
            case R.id.btn_start:
                recorder.setRecorderListener(waveView.getAmplitudeHelper());
                recorder.startRecorde();
                break;
                default:
                    break;
        }
    }
}
