package com.aurora.store.model;

import com.aurora.store.Constants;

public class Filter {

    private boolean systemApps;
    private boolean appsWithAds;
    private boolean paidApps;
    private boolean gsfDependentApps;
    private String category = Constants.TOP;
    private float rating;
    private int downloads;

    public boolean isSystemApps() {
        return systemApps;
    }

    public void setSystemApps(boolean systemApps) {
        this.systemApps = systemApps;
    }

    public boolean isAppsWithAds() {
        return appsWithAds;
    }

    public void setAppsWithAds(boolean appsWithAds) {
        this.appsWithAds = appsWithAds;
    }

    public boolean isPaidApps() {
        return paidApps;
    }

    public void setPaidApps(boolean paidApps) {
        this.paidApps = paidApps;
    }

    public boolean isGsfDependentApps() {
        return gsfDependentApps;
    }

    public void setGsfDependentApps(boolean gsfDependentApps) {
        this.gsfDependentApps = gsfDependentApps;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getDownloads() {
        return downloads;
    }

    public void setDownloads(int downloads) {
        this.downloads = downloads;
    }
}
