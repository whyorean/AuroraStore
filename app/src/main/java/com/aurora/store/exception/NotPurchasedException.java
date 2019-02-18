package com.aurora.store.exception;

import java.io.IOException;

public class NotPurchasedException extends IOException {

    public NotPurchasedException() {
        super("NotPurchasedException");
    }

    public NotPurchasedException(String message) {
        super(message);
    }

    public NotPurchasedException(String message, Throwable cause) {
        super(message, cause);
    }
}
