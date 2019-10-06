package com.aurora.store.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lombok.Data;

@Data
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
}
