package com.aurora.store.exception;

import com.dragons.aurora.playstoreapiv2.AuthException;

public class CaptchaException extends AuthException {

    private String loginToken;
    private String captchaURL;

    public CaptchaException(String message) {
        super(message);
    }

    public CaptchaException(String message, int code) {
        super(message);
        setCode(code);
    }

    public String getLoginToken() {
        return loginToken;
    }

    public void setLoginToken(String loginToken) {
        this.loginToken = loginToken;
    }

    public String getCaptchaURL() {
        return captchaURL;
    }

    public void setCaptchaURL(String captchaURL) {
        this.captchaURL = captchaURL;
    }
}
