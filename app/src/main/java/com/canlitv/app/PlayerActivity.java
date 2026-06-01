package com.canlitv.app;

import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class PlayerActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar progressBar;
    private String channelName;
    private String pageUrl;
    private String logoUrl;
    private TextView channelNameView;
    private ImageView logoImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        
        setContentView(R.layout.activity_player);

        channelName = getIntent().getStringExtra("channel_name");
        pageUrl = getIntent().getStringExtra("page_url");
        logoUrl = getIntent().getStringExtra("logo_url");

        playerView = findViewById(R.id.player_view);
        progressBar = findViewById(R.id.progress_bar);
        channelNameView = findViewById(R.id.channel_name);
        logoImageView = findViewById(R.id.channel_logo);

        if (channelNameView != null) {
            channelNameView.setText(channelName != null ? channelName : "Kanal Açılıyor...");
        }

        if (logoImageView != null && logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(this).load(logoUrl).into(logoImageView);
        }

        setTitle(channelName != null ? channelName : "Oynatıcı");

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);

        if (pageUrl != null) {
            loadChannelStream(pageUrl);
        }
    }

    private void loadChannelStream(String url) {
        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .timeout(30000)
                        .get();

                String videoUrl = null;

                // Try to extract M3U8 URL from script tags
                var scripts = doc.select("script");
                for (var script : scripts) {
                    String content = script.html();
                    if (content.contains(".m3u8") || content.contains("hls")) {
                        int start = content.indexOf("http");
                        if (start != -1) {
                            int end = content.indexOf("\"", start);
                            if (end == -1) end = content.indexOf("'", start);
                            if (end == -1) end = content.indexOf(";", start);
                            if (end > start) {
                                videoUrl = content.substring(start, end).trim();
                                if (videoUrl.contains(".m3u8") || videoUrl.contains(".mp4")) {
                                    break;
                                }
                            }
                        }
                    }
                }

                // Try video tag
                if (videoUrl == null) {
                    var videos = doc.select("video");
                    if (!videos.isEmpty()) {
                        var sources = videos.get(0).select("source");
                        if (!sources.isEmpty()) {
                            videoUrl = sources.get(0).attr("src");
                        }
                    }
                }

                // Try iframe
                if (videoUrl == null) {
                    var iframes = doc.select("iframe");
                    if (!iframes.isEmpty()) {
                        videoUrl = iframes.get(0).attr("src");
                    }
                }

                // Try data attributes
                if (videoUrl == null) {
                    var players = doc.select("[data-src], [data-video], .video-source, .stream-url");
                    if (!players.isEmpty()) {
                        videoUrl = players.get(0).attr("data-src");
                        if (videoUrl.isEmpty()) {
                            videoUrl = players.get(0).attr("data-video");
                        }
                    }
                }

                if (videoUrl != null && !videoUrl.isEmpty()) {
                    // Make URL absolute
                    if (videoUrl.startsWith("/")) {
                        String baseUrl = url.split("/live/")[0];
                        videoUrl = baseUrl + videoUrl;
                    } else if (!videoUrl.startsWith("http")) {
                        videoUrl = url.substring(0, url.lastIndexOf('/') + 1) + videoUrl;
                    }

                    String finalVideoUrl = videoUrl;
                    runOnUiThread(() -> playStream(finalVideoUrl));
                } else {
                    runOnUiThread(this::showError);
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(this::showError);
            }
        }).start();
    }

    private void playStream(String videoUrl) {
        if (player == null) return;

        try {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }

            MediaItem mediaItem = MediaItem.fromUri(videoUrl);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();

            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError();
        }
    }

    private void showError() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (channelNameView != null) {
            channelNameView.setText("Video yüklenirken hata oluştu. Lütfen tekrar deneyin.");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.play();
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
