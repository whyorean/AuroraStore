package com.aurora.store.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.aurora.store.Constants;
import com.aurora.store.TokenDispenserMirrors;
import com.aurora.store.adapter.OkHttpClientAdapter;
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

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Locale;

public class ApiBuilderUtil {

    public static GooglePlayAPI buildFromPreferences(Context context) throws Exception {
        LoginInfo loginInfo = LoginInfo.getSavedInstance(context);
        if (TextUtils.isEmpty(loginInfo.getEmail()) || TextUtils.isEmpty(loginInfo.getAuthToken())) {
            throw new CredentialsEmptyException();
        }

        PlayStoreApiBuilder builder = getBuilder(context, loginInfo);
        return builder.build();
    }

    public static GooglePlayAPI generateApiWithNewAuthToken(Context context) throws Exception {
        GooglePlayAPI api;
        LoginInfo loginInfo;
        if (Accountant.isAnonymous(context)) {
            api = login(context);
            loginInfo = LoginInfo.getSavedInstance(context);
        } else {
            loginInfo = LoginInfo.getSavedInstance(context);
            loginInfo.setAuthToken(null);
            PlayStoreApiBuilder builder = getBuilder(context, loginInfo);
            api = builder.build();
        }
        if (api != null) {
            loginInfo.setGsfId(api.getGsfId());
            loginInfo.setAuthToken(api.getToken());
            loginInfo.setDfeCookie(api.getDfeCookie());
            loginInfo.setDeviceConfigToken(api.getDeviceConfigToken());
            loginInfo.setDeviceCheckinConsistencyToken(api.getDeviceCheckinConsistencyToken());
            LoginInfo.save(context, loginInfo);
        }
        Accountant.setAnonymous(context, Accountant.isAnonymous(context));
        return api;
    }

    public static GooglePlayAPI buildApi(Context context, LoginInfo loginInfo, boolean isAnonymous) throws IOException {
        try {
            PlayStoreApiBuilder builder = getBuilder(context, loginInfo);
            if (isAnonymous)
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
                Accountant.setAnonymous(context, isAnonymous);
            }
            return api;
        } catch (ApiBuilderException e) {
            throw new RuntimeException(e);
        }
    }

    private static PlayStoreApiBuilder getBuilder(Context context, LoginInfo loginInfo) {
        SharedPreferences sharedPreferences = Util.getPrefs(context);
        String locale = sharedPreferences.getString(Constants.PREFERENCE_SPOOF_LOCALE, StringUtils.EMPTY);
        loginInfo.setLocale(TextUtils.isEmpty(locale) ? Locale.getDefault().getLanguage() : locale);

        PlayStoreApiBuilder builder = new PlayStoreApiBuilder();
        builder.setHttpClient(new OkHttpClientAdapter(context));
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
        String spoofDevice = PrefUtil.getString(context, Constants.PREFERENCE_SPOOF_DEVICE);
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

    public static GooglePlayAPI login(Context context) throws IOException {
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setTokenDispenserUrl(TokenDispenserMirrors.get(context));
        return buildApi(context, loginInfo, true);
    }

    public static GooglePlayAPI getApi(Context context) throws Exception {
        return buildFromPreferences(context);
    }

    public static GooglePlayAPI getPlayApi(Context context) throws Exception {
        return buildFromPreferences(context);
    }

}
