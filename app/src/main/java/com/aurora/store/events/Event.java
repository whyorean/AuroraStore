package com.aurora.store.events;

import lombok.Data;


@Data
public class Event {

    private SubType subType;
    private String packageName;
    private int status;

    public Event(SubType subType, String packageName, int status) {
        this.subType = subType;
        this.packageName = packageName;
        this.status = status;
    }

    public Event(SubType subType, String packageName) {
        this.subType = subType;
        this.packageName = packageName;
    }

    public Event(SubType subType) {
        this.subType = subType;
    }

    public enum SubType {
        API_SUCCESS,
        API_FAILED,
        API_ERROR,
        BLACKLIST,
        WHITELIST,
        INSTALLED,
        UNINSTALLED,
        NETWORK_UNAVAILABLE,
        NETWORK_AVAILABLE,
        BULK_UPDATE_NOTIFY
    }
}
