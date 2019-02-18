package com.aurora.store.exception;

import com.dragons.aurora.playstoreapiv2.AuthException;

public class CredentialsEmptyException extends AuthException {

    public CredentialsEmptyException() {
        super("CredentialsEmptyException");
    }

    public CredentialsEmptyException(String message) {
        super(message);
    }

    public CredentialsEmptyException(String message, Throwable cause) {
        super(message, cause);
    }
}
