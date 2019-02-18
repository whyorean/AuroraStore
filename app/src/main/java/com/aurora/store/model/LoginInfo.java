package com.aurora.store.model;

import java.util.Locale;

public class LoginInfo {

    private String email;
    private String password;
    private String gsfId;
    private String token;
    private Locale locale;
    private String tokenDispenserUrl;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGsfId() {
        return gsfId;
    }

    public void setGsfId(String gsfId) {
        this.gsfId = gsfId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String getTokenDispenserUrl() {
        return tokenDispenserUrl;
    }

    public void setTokenDispenserUrl(String tokenDispenserUrl) {
        this.tokenDispenserUrl = tokenDispenserUrl;
    }
}
