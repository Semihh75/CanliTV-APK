package com.canlitv.app;

import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;

public class TVBridge {
    private Context context;

    public TVBridge(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void openChannel(String pageUrl, String channelName, String logoUrl) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra("page_url", pageUrl);
        intent.putExtra("channel_name", channelName);
        intent.putExtra("logo_url", logoUrl);
        context.startActivity(intent);
    }
}
