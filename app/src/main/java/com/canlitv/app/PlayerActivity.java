package com.canlitv.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    private WebView webView;
    private ProgressBar loadingSpinner;
    private View channelInfoOverlay;
    private Handler hideHandler = new Handler(Looper.getMainLooper());

    private String pageUrl;
    private boolean usingWebView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_player);

        playerView    = findViewById(R.id.playerView);
        webView       = findViewById(R.id.playerWebView);
        loadingSpinner = findViewById(R.id.playerLoading);
        channelInfoOverlay = findViewById(R.id.channelInfoOverlay);

        String channelName = getIntent().getStringExtra("channel_name");
        String channelLogo = getIntent().getStringExtra("channel_logo");
        String streamUrl   = getIntent().getStringExtra("stream_url");
        pageUrl            = getIntent().getStringExtra("page_url");

        // Set channel info
        TextView nameView = findViewById(R.id.playerChannelName);
        ImageView logoView = findViewById(R.id.playerLogo);
        if (channelName != null) nameView.setText(channelName);
        if (channelLogo != null) Glide.with(this).load(channelLogo).into(logoView);

        hideHandler.postDelayed(() -> channelInfoOverlay.setVisibility(View.GONE), 4000);

        // Try ExoPlayer first if we have a direct stream URL
        if (streamUrl != null && !streamUrl.isEmpty()) {
            tryExoPlayer(streamUrl);
        } else {
            // No stream URL found — go straight to WebView
            openWebView(pageUrl);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void tryExoPlayer(String url) {
        playerView.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setUseController(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    loadingSpinner.setVisibility(View.VISIBLE);
                } else if (state == Player.STATE_READY) {
                    loadingSpinner.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                // ExoPlayer failed — fall back to WebView
                releasePlayer();
                openWebView(pageUrl);
            }
        });

        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.play();
    }

    private void openWebView(String url) {
        usingWebView = true;
        playerView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        loadingSpinner.setVisibility(View.VISIBLE);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUserAgentString(
            "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) SamsungBrowser/2.1 Chrome/91.0.4472.124 TV Safari/537.36"
        );

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                loadingSpinner.setVisibility(View.GONE);

                // Inject CSS to hide ads, nav bars, popups — show only the video
                view.evaluateJavascript(
                    "(function() {" +
                    "  var style = document.createElement('style');" +
                    "  style.textContent = '" +
                    "    header, footer, nav, .navbar, .menu, .sidebar," +
                    "    .ad, .ads, .advertisement, [class*=\"ad-\"], [id*=\"ad-\"]," +
                    "    .cookie-banner, .popup, .overlay, .modal," +
                    "    [class*=\"popup\"], [class*=\"banner\"], [class*=\"cookie\"]" +
                    "    { display: none !important; }" +
                    "    body { margin: 0 !important; padding: 0 !important; background: #000 !important; }" +
                    "    video, iframe { width: 100vw !important; height: 100vh !important;" +
                    "      position: fixed !important; top: 0 !important; left: 0 !important;" +
                    "      z-index: 9999 !important; background: #000 !important; }" +
                    "  ';" +
                    "  document.head.appendChild(style);" +
                    "  // Auto-click play button if present" +
                    "  var playBtn = document.querySelector('.play-button, .vjs-play-control, [aria-label=\"Play\"], button.play');" +
                    "  if (playBtn) playBtn.click();" +
                    "})();",
                    null
                );
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        if (url != null) webView.loadUrl(url);
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (usingWebView && webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
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
        if (usingWebView) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) player.play();
        if (usingWebView) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        hideHandler.removeCallbacksAndMessages(null);
        releasePlayer();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
