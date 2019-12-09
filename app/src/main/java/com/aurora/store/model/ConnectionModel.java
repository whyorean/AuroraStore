package com.aurora.store.model;

import lombok.Data;

@Data
public class ConnectionModel {
    private String typeName;
    private boolean isConnected;

    public ConnectionModel(String typeName, boolean isConnected) {
        this.typeName = typeName;
        this.isConnected = isConnected;
    }
}
