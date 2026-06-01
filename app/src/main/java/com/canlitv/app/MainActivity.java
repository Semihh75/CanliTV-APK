package com.canlitv.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView channelGrid;
    private View loadingLayout;
    private View errorLayout;
    private TextView channelCount;
    private TextView retryButton;

    private ChannelScraper scraper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        channelGrid = findViewById(R.id.channelGrid);
        loadingLayout = findViewById(R.id.loadingLayout);
        errorLayout = findViewById(R.id.errorLayout);
        channelCount = findViewById(R.id.channelCount);
        retryButton = findViewById(R.id.retryButton);

        // 5 columns for TV (1080p)
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 5);
        channelGrid.setLayoutManager(gridLayoutManager);

        scraper = new ChannelScraper();

        retryButton.setOnClickListener(v -> loadChannels());
        retryButton.setOnFocusChangeListener((v, hasFocus) -> {
            retryButton.setAlpha(hasFocus ? 0.7f : 1.0f);
        });

        loadChannels();
    }

    private void loadChannels() {
        showLoading();

        scraper.scrapeChannels(new ChannelScraper.ScrapeCallback() {
            @Override
            public void onSuccess(List<Channel> channels) {
                runOnUiThread(() -> {
                    channelCount.setText(channels.size() + " kanal");

                    ChannelAdapter adapter = new ChannelAdapter(
                        MainActivity.this,
                        channels,
                        channel -> openChannel(channel)
                    );

                    channelGrid.setAdapter(adapter);
                    showGrid();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showError(message));
            }
        });
    }

    private void openChannel(Channel channel) {
        // First try to resolve stream URL, then open player
        scraper.resolveStreamUrl(channel, new ChannelScraper.StreamCallback() {
            @Override
            public void onSuccess(String streamUrl) {
                channel.streamUrl = streamUrl;
                runOnUiThread(() -> launchPlayer(channel));
            }

            @Override
            public void onFallback(String pageUrl) {
                // No direct stream found — open WebView fallback
                runOnUiThread(() -> launchPlayer(channel));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> launchPlayer(channel));
            }
        });
    }

    private void launchPlayer(Channel channel) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("channel_name", channel.name);
        intent.putExtra("channel_logo", channel.logoUrl);
        intent.putExtra("stream_url", channel.streamUrl);
        intent.putExtra("page_url", channel.pageUrl);
        startActivity(intent);
    }

    private void showLoading() {
        loadingLayout.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        channelGrid.setVisibility(View.GONE);
    }

    private void showGrid() {
        loadingLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
        channelGrid.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        loadingLayout.setVisibility(View.GONE);
        channelGrid.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        TextView errorText = findViewById(R.id.errorText);
        if (message != null) errorText.setText(message);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
