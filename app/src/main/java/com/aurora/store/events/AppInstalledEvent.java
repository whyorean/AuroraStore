package com.aurora.store.events;

public class AppInstalledEvent {

    private String packageName;

    public AppInstalledEvent(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
