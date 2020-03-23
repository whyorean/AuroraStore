package com.aurora.store.events;

import lombok.Data;


@Data
public class Event {

    private SubType subType;
    private String stringExtra;
    private int intExtra;
    private int status;

    public Event(SubType subType, String stringExtra, int status) {
        this.subType = subType;
        this.stringExtra = stringExtra;
        this.status = status;
    }

    public Event(SubType subType, String stringExtra) {
        this.subType = subType;
        this.stringExtra = stringExtra;
    }

    public Event(SubType subType, int intExtra) {
        this.subType = subType;
        this.intExtra = intExtra;
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
