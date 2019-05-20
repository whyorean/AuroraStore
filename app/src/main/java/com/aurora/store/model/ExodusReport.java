package com.aurora.store.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

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

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Report> getReports() {
        return reports;
    }

    public void setReports(List<Report> reports) {
        this.reports = reports;
    }

}
