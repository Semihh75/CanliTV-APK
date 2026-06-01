package com.canlitv.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;

public class PlayerActivity extends Activity {

    private ExoPlayer player;
    private PlayerView playerView;
    private ProgressBar loadingSpinner;
    private View channelInfoOverlay;
    private Handler hideHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on while watching
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.playerView);
        loadingSpinner = findViewById(R.id.playerLoading);
        channelInfoOverlay = findViewById(R.id.channelInfoOverlay);

        String channelName = getIntent().getStringExtra("channel_name");
        String channelLogo = getIntent().getStringExtra("channel_logo");
        String streamUrl   = getIntent().getStringExtra("stream_url");
        String pageUrl     = getIntent().getStringExtra("page_url");

        // Set channel info overlay
        TextView nameView = findViewById(R.id.playerChannelName);
        ImageView logoView = findViewById(R.id.playerLogo);
        if (channelName != null) nameView.setText(channelName);
        if (channelLogo != null) {
            Glide.with(this).load(channelLogo).into(logoView);
        }

        // Auto-hide overlay after 4 seconds
        hideHandler.postDelayed(() -> channelInfoOverlay.setVisibility(View.GONE), 4000);

        if (streamUrl != null && !streamUrl.isEmpty()) {
            playStream(streamUrl);
        } else if (pageUrl != null) {
            // Fallback: no direct stream found, play page URL (might work with HLS detection)
            playStream(pageUrl);
        } else {
            finish();
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void playStream(String url) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setUseController(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    loadingSpinner.setVisibility(View.VISIBLE);
                } else {
                    loadingSpinner.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                loadingSpinner.setVisibility(View.GONE);
                // Player error — go back to channel list
                finish();
            }
        });

        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        // Show overlay on any key press
        channelInfoOverlay.setVisibility(View.VISIBLE);
        hideHandler.removeCallbacksAndMessages(null);
        hideHandler.postDelayed(() -> channelInfoOverlay.setVisibility(View.GONE), 4000);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) player.play();
    }

    @Override
    protected void onDestroy() {
        hideHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
