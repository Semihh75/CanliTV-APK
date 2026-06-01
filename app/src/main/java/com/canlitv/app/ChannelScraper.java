package com.canlitv.app;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChannelScraper {

    private static final String TAG = "ChannelScraper";
    private static final String BASE_URL = "https://www.canlitv.me";
    private static final String LIVE_URL = "https://www.canlitv.me/live";

    private final OkHttpClient client;

    public ChannelScraper() {
        client = new OkHttpClient.Builder()
            .followRedirects(true)
            .build();
    }

    public interface ScrapeCallback {
        void onSuccess(List<Channel> channels);
        void onError(String message);
    }

    public void scrapeChannels(ScrapeCallback callback) {
        new Thread(() -> {
            try {
                String html = fetchHtml(LIVE_URL);
                if (html == null) {
                    callback.onError("Sayfa yüklenemedi");
                    return;
                }

                List<Channel> channels = parseChannels(html);

                if (channels.isEmpty()) {
                    callback.onError("Kanal bulunamadı");
                } else {
                    callback.onSuccess(channels);
                }
            } catch (Exception e) {
                Log.e(TAG, "Scrape error", e);
                callback.onError("Hata: " + e.getMessage());
            }
        }).start();
    }

    public void resolveStreamUrl(Channel channel, StreamCallback callback) {
        new Thread(() -> {
            try {
                String html = fetchHtml(channel.pageUrl);
                if (html == null) {
                    callback.onError("Sayfa yüklenemedi");
                    return;
                }

                String streamUrl = extractStreamUrl(html);
                if (streamUrl != null) {
                    callback.onSuccess(streamUrl);
                } else {
                    // Fallback: open the page in WebView
                    callback.onFallback(channel.pageUrl);
                }
            } catch (Exception e) {
                Log.e(TAG, "Stream resolve error", e);
                callback.onFallback(channel.pageUrl);
            }
        }).start();
    }

    private String fetchHtml(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("Accept-Language", "tr-TR,tr;q=0.9,en;q=0.8")
            .addHeader("Referer", "https://www.canlitv.me/")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
        }
        return null;
    }

    private List<Channel> parseChannels(String html) {
        List<Channel> channels = new ArrayList<>();

        // Pattern 1: Look for channel links with logos
        // Typical pattern: <a href="/kanal-adi"><img src="logo.png" alt="Kanal Adı" /></a>
        Pattern pattern = Pattern.compile(
            "<a[^>]+href=[\"']([^\"']*(?:/[a-z0-9-]+)?)[\"'][^>]*>\\s*" +
            "<img[^>]+src=[\"']([^\"']+)[\"'][^>]*alt=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String href = matcher.group(1);
            String imgSrc = matcher.group(2);
            String altText = matcher.group(3);

            if (href == null || imgSrc == null || altText == null) continue;

            // Filter out non-channel links
            if (href.contains("live") || href.contains("kanal") || href.contains("tv") ||
                href.matches(".*/[a-z0-9-]{3,}$")) {

                String fullUrl = href.startsWith("http") ? href : BASE_URL + href;
                String fullLogo = imgSrc.startsWith("http") ? imgSrc : BASE_URL + imgSrc;

                // Skip ads, icons, banners
                if (imgSrc.contains("ad") || imgSrc.contains("banner") || imgSrc.contains("logo-site")) continue;
                if (altText.length() < 2 || altText.length() > 50) continue;

                channels.add(new Channel(altText.trim(), fullLogo, fullUrl));
            }
        }

        // Pattern 2: Alternative — img then name in nearby span/div
        if (channels.isEmpty()) {
            Pattern p2 = Pattern.compile(
                "<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>[^<]*" +
                "<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>[^<]*" +
                "<(?:span|div|p)[^>]*>([^<]{2,40})</(?:span|div|p)>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );
            Matcher m2 = p2.matcher(html);
            while (m2.find()) {
                String href = m2.group(1);
                String imgSrc = m2.group(2);
                String name = m2.group(3).trim();
                if (href == null || imgSrc == null) continue;
                String fullUrl = href.startsWith("http") ? href : BASE_URL + href;
                String fullLogo = imgSrc.startsWith("http") ? imgSrc : BASE_URL + imgSrc;
                channels.add(new Channel(name, fullLogo, fullUrl));
            }
        }

        return channels;
    }

    private String extractStreamUrl(String html) {
        // Try to find .m3u8 stream URL
        String[] patterns = {
            "\"(https?://[^\"]+\\.m3u8[^\"]*)\"|'(https?://[^']+\\.m3u8[^']*)'",
            "source:\\s*[\"'](https?://[^\"']+)[\"']",
            "file:\\s*[\"'](https?://[^\"']+)[\"']",
            "src=[\"'](https?://[^\"']+\\.m3u8[^\"']*)[\"']",
        };

        for (String p : patterns) {
            Matcher m = Pattern.compile(p, Pattern.CASE_INSENSITIVE).matcher(html);
            if (m.find()) {
                String url = m.group(1) != null ? m.group(1) : m.group(2);
                if (url != null && !url.isEmpty()) return url;
            }
        }
        return null;
    }

    public interface StreamCallback {
        void onSuccess(String streamUrl);
        void onFallback(String pageUrl);
        void onError(String message);
    }
}
