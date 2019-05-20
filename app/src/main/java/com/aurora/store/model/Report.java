package com.aurora.store.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Report {
    @SerializedName("downloads")
    @Expose
    private String downloads;
    @SerializedName("version")
    @Expose
    private String version;
    @SerializedName("creation_date")
    @Expose
    private String creationDate;
    @SerializedName("updated_at")
    @Expose
    private String updatedAt;
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("version_code")
    @Expose
    private String versionCode;
    @SerializedName("trackers")
    @Expose
    private List<Integer> trackers = null;

    public String getDownloads() {
        return downloads;
    }

    public void setDownloads(String downloads) {
        this.downloads = downloads;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getCreationDate() {
        try {
            DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    Locale.getDefault());
            return simpleDateFormat.parse(creationDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public List<Integer> getTrackers() {
        return trackers;
    }

    public void setTrackers(List<Integer> trackers) {
        this.trackers = trackers;
    }


}
