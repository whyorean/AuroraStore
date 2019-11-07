package com.aurora.store.api;

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
    private String aasToken;
    private String authToken;
    private String gsfId;
    private Locale locale;
    private String tokenDispenserUrl;
    private String deviceCheckinConsistencyToken;
    private String deviceConfigToken;
    private String dfeCookie;

    private transient DeviceInfoProvider deviceInfoProvider;
    private transient HttpClientAdapter httpClient;
    private transient TokenDispenserClient tokenDispenserClient;


    public GooglePlayAPI build() throws IOException, ApiBuilderException {
        return buildUpon(new GooglePlayAPI());
    }

    private GooglePlayAPI buildUpon(GooglePlayAPI api) throws IOException, ApiBuilderException {
        if (null == httpClient) {
            throw new ApiBuilderException("HttpClientAdapter is required");
        }

        if (null == deviceInfoProvider) {
            throw new ApiBuilderException("DeviceInfoProvider is required");
        }

        api.setLocale(null == locale ? Locale.getDefault() : locale);
        api.setClient(httpClient);
        api.setDeviceInfoProvider(deviceInfoProvider);

        if (isEmpty(aasToken) && isEmpty(authToken) && isEmpty(tokenDispenserUrl)) {
            throw new ApiBuilderException("Email-aasToken pair, a authToken or a authToken dispenser url is required");
        } else {
            if (!isEmpty(tokenDispenserUrl)) {
                tokenDispenserClient = new TokenDispenserClient(tokenDispenserUrl, httpClient);
            }

            if ((isEmpty(authToken) || isEmpty(gsfId)) && isEmpty(email) && null != tokenDispenserClient) {
                email = tokenDispenserClient.getRandomEmail();
                if (isEmpty(email)) {
                    throw new ApiBuilderException("Could not get email from authToken dispenser");
                }
            }

            if (isEmpty(email) && (isEmpty(authToken) || isEmpty(gsfId))) {
                throw new ApiBuilderException("Email is required");
            } else {
                boolean needToUploadDeviceConfig = false;
                if (isEmpty(gsfId)) {
                    gsfId = generateGsfId(api);
                    needToUploadDeviceConfig = true;
                }

                api.setGsfId(gsfId);

                if (isEmpty(authToken)) {
                    authToken = generateToken(api);
                }

                api.setToken(authToken);

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
        }
    }

    private String generateGsfId(GooglePlayAPI api) throws IOException {
        return api.generateGsfId();
    }

    private String generateToken(GooglePlayAPI api) throws IOException {
        return isEmpty(aasToken) ? tokenDispenserClient.getToken(email) : api.generateToken(email, aasToken);
    }

    private boolean isEmpty(String value) {
        return null == value || value.length() == 0;
    }
}
