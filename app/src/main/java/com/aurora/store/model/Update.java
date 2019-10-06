package com.aurora.store.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
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
}