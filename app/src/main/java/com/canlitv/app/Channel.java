package com.canlitv.app;

public class Channel {
    public String name;
    public String logoUrl;
    public String pageUrl;   // URL of the channel page on canlitv.me
    public String streamUrl; // Actual .m3u8 or stream URL (resolved later)

    public Channel(String name, String logoUrl, String pageUrl) {
        this.name = name;
        this.logoUrl = logoUrl;
        this.pageUrl = pageUrl;
    }
}
