package com.aurora.store.model;

import android.content.pm.ApplicationInfo;

public class ImageSource {

    private String url;
    private ApplicationInfo applicationInfo;
    private boolean fullSize;

    public ImageSource() {
    }

    public ImageSource(String url) {
        setUrl(url);
    }

    public ImageSource(ApplicationInfo applicationInfo) {
        setApplicationInfo(applicationInfo);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public void setApplicationInfo(ApplicationInfo applicationInfo) {
        this.applicationInfo = applicationInfo;
    }

    public boolean isFullSize() {
        return fullSize;
    }

    public ImageSource setFullSize(boolean fullSize) {
        this.fullSize = fullSize;
        return this;
    }
}
