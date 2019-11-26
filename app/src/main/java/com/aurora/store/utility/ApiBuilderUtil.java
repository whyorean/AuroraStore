package com.aurora.store.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.aurora.store.Constants;
import com.aurora.store.adapter.NativeHttpClientAdapter;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.manager.LocaleManager;
import com.aurora.store.manager.SpoofManager;
import com.aurora.store.model.LoginInfo;
import com.aurora.store.provider.NativeDeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.ApiBuilderException;
import com.dragons.aurora.playstoreapiv2.DeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.PlayStoreApiBuilder;
import com.dragons.aurora.playstoreapiv2.PropertiesDeviceInfoProvider;

import java.io.IOException;
import java.util.Locale;

public class ApiBuilderUtil {

    public static GooglePlayAPI buildFromPreferences(Context context) throws IOException {
        LoginInfo loginInfo = LoginInfo.getSavedInstance(context);
        if (TextUtils.isEmpty(loginInfo.getEmail()) || TextUtils.isEmpty(loginInfo.getAuthToken())) {
            throw new CredentialsEmptyException();
        }
        if (Accountant.isAnonymous(context))
            return buildAnonymousApi(context, loginInfo);
        else
            return buildApi(context, loginInfo);
    }

    public static GooglePlayAPI buildAnonymousApi(Context context, LoginInfo loginInfo) throws IOException {
        try {
            PlayStoreApiBuilder builder = getBuilder(context, loginInfo);
            builder.setTokenDispenserUrl(loginInfo.getTokenDispenserUrl());
            GooglePlayAPI api = builder.build();
            loginInfo.setEmail(builder.getEmail());
            if (api != null) {
                loginInfo.setGsfId(api.getGsfId());
                loginInfo.setAuthToken(api.getToken());
                loginInfo.setDfeCookie(api.getDfeCookie());
                loginInfo.setDeviceConfigToken(api.getDeviceConfigToken());
                loginInfo.setDeviceCheckinConsistencyToken(api.getDeviceCheckinConsistencyToken());
                LoginInfo.save(context, loginInfo);
                Accountant.setAnonymous(context, true);
            }
            return api;
        } catch (ApiBuilderException e) {
            throw new RuntimeException(e);
        }
    }

    public static GooglePlayAPI buildApi(Context context, LoginInfo loginInfo) throws IOException {
        try {
            PlayStoreApiBuilder builder = getBuilder(context, loginInfo);
            GooglePlayAPI api = builder.build();
            loginInfo.setEmail(builder.getEmail());
            if (api != null) {
                loginInfo.setGsfId(api.getGsfId());
                loginInfo.setAuthToken(api.getToken());
                loginInfo.setDfeCookie(api.getDfeCookie());
                loginInfo.setDeviceConfigToken(api.getDeviceConfigToken());
                loginInfo.setDeviceCheckinConsistencyToken(api.getDeviceCheckinConsistencyToken());
                LoginInfo.save(context, loginInfo);
                Accountant.setAnonymous(context, false);
            }
            return api;
        } catch (ApiBuilderException e) {
            throw new RuntimeException(e);
        }
    }

    private static PlayStoreApiBuilder getBuilder(Context context, LoginInfo loginInfo) {
        SharedPreferences prefs = Util.getPrefs(context);
        String locale = prefs.getString(Constants.PREFERENCE_REQUESTED_LANGUAGE, "");
        loginInfo.setLocale(TextUtils.isEmpty(locale) ? Locale.getDefault().toString() : locale);

        PlayStoreApiBuilder builder = new PlayStoreApiBuilder();
        builder.setHttpClient(new NativeHttpClientAdapter(context));
        builder.setDeviceInfoProvider(getDeviceInfoProvider(context));
        builder.setLocale(loginInfo.getLocale());
        builder.setEmail(loginInfo.getEmail());
        builder.setAasToken(loginInfo.getAasToken());
        builder.setGsfId(loginInfo.getGsfId());
        builder.setAuthToken(loginInfo.getAuthToken());
        builder.setDeviceCheckinConsistencyToken(loginInfo.getDeviceCheckinConsistencyToken());
        builder.setDeviceConfigToken(loginInfo.getDeviceConfigToken());
        builder.setDfeCookie(loginInfo.getDfeCookie());
        return builder;
    }

    public static DeviceInfoProvider getDeviceInfoProvider(Context context) {
        DeviceInfoProvider deviceInfoProvider;
        String spoofDevice = PrefUtil.getString(context, Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE);
        if (TextUtils.isEmpty(spoofDevice)) {
            deviceInfoProvider = new NativeDeviceInfoProvider();
            ((NativeDeviceInfoProvider) deviceInfoProvider).setContext(context);
            ((NativeDeviceInfoProvider) deviceInfoProvider).setLocaleString(new LocaleManager(context).getLocale().toString());
        } else {
            deviceInfoProvider = new PropertiesDeviceInfoProvider();
            ((PropertiesDeviceInfoProvider) deviceInfoProvider).setProperties(new SpoofManager(context).getProperties(spoofDevice));
            ((PropertiesDeviceInfoProvider) deviceInfoProvider).setLocaleString(new LocaleManager(context).getLocale().toString());
        }
        return deviceInfoProvider;
    }

}
