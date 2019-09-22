package com.aurora.store.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Update {
    @SerializedName("version_name")
    @Expose
    private String versionName;
    @SerializedName("version_code")
    @Expose
    private Integer versionCode;
    @SerializedName("aurora_build")
    @Expose
    private String auroraBuild;
    @SerializedName("fdroid_build")
    @Expose
    private String fdroidBuild;
    @SerializedName("changelog")
    @Expose
    private String changelog;

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public Integer getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(Integer versionCode) {
        this.versionCode = versionCode;
    }

    public String getAuroraBuild() {
        return auroraBuild;
    }

    public void setAuroraBuild(String auroraBuild) {
        this.auroraBuild = auroraBuild;
    }

    public String getFdroidBuild() {
        return fdroidBuild;
    }

    public void setFdroidBuild(String fdroidBuild) {
        this.fdroidBuild = fdroidBuild;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }
}