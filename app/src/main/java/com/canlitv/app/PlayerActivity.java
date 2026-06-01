package com.canlitv.app;

import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;

public class PlayerActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        webView = findViewById(R.id.player_webview);
        
        // Configure WebView for video playback
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setPluginState(WebSettings.PluginState.ON);

        // Custom WebViewClient for better video loading
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Inject auto-play script
                webView.evaluateJavascript(
                    "javascript:void((function(){" +
                    "var videos=document.querySelectorAll('video,iframe');" +
                    "videos.forEach(v=>{v.autoplay=true;v.muted=false;});" +
                    "})()",
                    null
                );
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(android.view.View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);
                // Fullscreen video support
                FrameLayout container = findViewById(R.id.video_container);
                container.removeAllViews();
                container.addView(view);
            }
        });

        // Get channel data from intent
        String pageUrl = getIntent().getStringExtra("page_url");
        String channelName = getIntent().getStringExtra("channel_name");

        setTitle(channelName != null ? channelName : "Kanal Açılıyor...");

        // Load the channel page
        if (pageUrl != null) {
            webView.loadUrl(pageUrl);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            } else {
                finish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.resumeTimers();
            webView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
