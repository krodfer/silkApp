package com.example.ufabcirco.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ufabcirco.R;

public class FullscreenVideoActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URL = "video_url";
    private VideoView fullscreenVideoView;
    private MediaController mediaController;
    private String videoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_video);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        fullscreenVideoView = findViewById(R.id.fullscreen_video_view);
        videoUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);

        if (videoUrl != null) {
            fullscreenVideoView.setVideoURI(Uri.parse(videoUrl));

            mediaController = new MediaController(this);
            fullscreenVideoView.setMediaController(mediaController);
            mediaController.setAnchorView(fullscreenVideoView);
            mediaController.setVisibility(View.GONE);

            fullscreenVideoView.setOnPreparedListener(mp -> {
                fullscreenVideoView.start();
                mediaController.setVisibility(View.VISIBLE);
            });

            fullscreenVideoView.setOnClickListener(v -> {
                if (mediaController.isShowing()) {
                    mediaController.hide();
                } else {
                    mediaController.show();
                }
            });

        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }
}