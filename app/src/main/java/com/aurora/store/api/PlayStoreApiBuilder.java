package com.aurora.store.api;

import com.aurora.store.utility.Log;
import com.dragons.aurora.playstoreapiv2.ApiBuilderException;
import com.dragons.aurora.playstoreapiv2.DeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.HttpClientAdapter;
import com.dragons.aurora.playstoreapiv2.TokenDispenserClient;

import java.io.IOException;
import java.util.Locale;

import lombok.Data;

@Data
public class PlayStoreApiBuilder {
    private String email;
    private String password;
    private String gsfId;
    private String token;
    private Locale locale;
    private DeviceInfoProvider deviceInfoProvider;
    private HttpClientAdapter httpClient;
    private String tokenDispenserUrl;
    private TokenDispenserClient tokenDispenserClient;
    private String deviceCheckinConsistencyToken;
    private String deviceConfigToken;
    private String dfeCookie;
    private String loginToken;
    private String loginCaptcha;

    public GooglePlayAPI build() throws IOException, ApiBuilderException {
        return buildUpon(new GooglePlayAPI());
    }

    public GooglePlayAPI buildUpon(GooglePlayAPI api) throws IOException, ApiBuilderException {
        api.setLocale(null == locale ? Locale.getDefault() : locale);
        api.setClient(httpClient);
        if (null == httpClient) {
            throw new ApiBuilderException("HttpClientAdapter is required");
        } else {
            api.setClient(httpClient);
        }
        if (null == deviceInfoProvider) {
            throw new ApiBuilderException("DeviceInfoProvider is required");
        } else {
            api.setDeviceInfoProvider(deviceInfoProvider);
        }
        if (isEmpty(password) && isEmpty(token) && isEmpty(tokenDispenserUrl)) {
            throw new ApiBuilderException("Email-password pair, a token or a token dispenser url is required");
        }
        if (!isEmpty(tokenDispenserUrl)) {
            tokenDispenserClient = new TokenDispenserClient(tokenDispenserUrl, httpClient);
        }
        if ((isEmpty(token) || isEmpty(gsfId)) && isEmpty(email) && null != tokenDispenserClient) {
            email = tokenDispenserClient.getRandomEmail();
            if (isEmpty(email)) {
                throw new ApiBuilderException("Could not get email from token dispenser");
            }
        }
        if (isEmpty(email) && (isEmpty(token) || isEmpty(gsfId))) {
            throw new ApiBuilderException("Email is required");
        }
        boolean needToUploadDeviceConfig = false;
        if (isEmpty(gsfId)) {
            gsfId = generateGsfId(api);
            needToUploadDeviceConfig = true;
        }
        api.setGsfId(gsfId);
        if (isEmpty(token)) {
            token = generateToken(api);
            Log.e(token);
        }
        api.setToken(token);
        if (needToUploadDeviceConfig) {
            api.uploadDeviceConfig();
        }
        if (isEmpty(api.getDeviceCheckinConsistencyToken())) {
            api.setDeviceCheckinConsistencyToken(deviceCheckinConsistencyToken);
        }
        if (isEmpty(api.getDeviceConfigToken())) {
            api.setDeviceConfigToken(deviceConfigToken);
        }
        if (isEmpty(api.getDfeCookie())) {
            api.setDfeCookie(dfeCookie);
        }
        return api;
    }

    private String generateGsfId(GooglePlayAPI api) throws IOException {
        Log.e("Called");
        String tokenAc2dm;
        if (!isEmpty(loginCaptcha) && !isEmpty(loginToken)) {
            Log.e("MKL");
            tokenAc2dm =  api.generateAC2DMToken(email, password, loginToken, loginCaptcha);
            Log.e("MKC");
        }
        else
            tokenAc2dm = isEmpty(password) ? tokenDispenserClient.getTokenAc2dm(email) : api.generateAC2DMToken(email, password);
        return api.generateGsfId(email, tokenAc2dm);
    }

    private String generateToken(GooglePlayAPI api) throws IOException {
        Log.e("Calledsdsd");
        if (!isEmpty(loginCaptcha) && !isEmpty(loginToken)) {
            return api.generateToken(email, password, loginToken, loginCaptcha);
        }
        return isEmpty(password) ? tokenDispenserClient.getToken(email) : api.generateToken(email, password);
    }

    private boolean isEmpty(String value) {
        return null == value || value.length() == 0;
    }
}
