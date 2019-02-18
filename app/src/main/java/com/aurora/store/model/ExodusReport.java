package com.aurora.store.model;

import org.json.JSONArray;

public class ExodusReport {

    private String appId;
    private String version;
    private String versionCode;
    private JSONArray trackerIds;
    private App app;

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public JSONArray getTrackerIds() {
        return trackerIds;
    }

    public void setTrackerIds(JSONArray trackerIds) {
        this.trackerIds = trackerIds;
    }
}
