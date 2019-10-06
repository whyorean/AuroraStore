package com.aurora.store.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ExodusReport {

    @SerializedName("creator")
    @Expose
    private String creator;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("reports")
    @Expose
    private List<Report> reports = new ArrayList<>();
}
