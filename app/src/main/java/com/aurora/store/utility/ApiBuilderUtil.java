package com.aurora.store.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.aurora.store.Constants;
import com.aurora.store.adapter.NativeHttpClientAdapter;
import com.aurora.store.adapter.OkHttpClientAdapter;
import com.aurora.store.api.PlayStoreApiBuilder;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.manager.LocaleManager;
import com.aurora.store.manager.SpoofManager;
import com.aurora.store.model.LoginInfo;
import com.aurora.store.provider.NativeDeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.ApiBuilderException;
import com.dragons.aurora.playstoreapiv2.DeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.PropertiesDeviceInfoProvider;

import java.io.IOException;
import java.util.Locale;

public class ApiBuilderUtil {

    public static GooglePlayAPI buildFromPreferences(Context context) throws IOException {
        SharedPreferences prefs = Util.getPrefs(context);
        String email = prefs.getString(Accountant.ACCOUNT_EMAIL, "");
        if (TextUtils.isEmpty(email)) {
            throw new CredentialsEmptyException();
        }
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setEmail(email);
        return build2(context, loginInfo);
    }

    public static GooglePlayAPI build2(Context context, LoginInfo loginInfo) throws IOException {
        try {
            PlayStoreApiBuilder builder = getBuilder(context, loginInfo);
            GooglePlayAPI api = builder.build();
            loginInfo.setEmail(builder.getEmail());
            if (api != null) {
                loginInfo.setGsfId(api.getGsfId());
                loginInfo.setToken(api.getToken());
                loginInfo.setDfeCookie(api.getDfeCookie());
                loginInfo.setDeviceConfigToken(api.getDeviceConfigToken());
                loginInfo.setDeviceCheckinConsistencyToken(api.getDeviceCheckinConsistencyToken());
                saveLoginInfo(context, loginInfo);
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
        loginInfo.setGsfId(prefs.getString(Accountant.GSF_ID, ""));
        loginInfo.setToken(prefs.getString(Accountant.AUTH_TOKEN, ""));

        PlayStoreApiBuilder builder = new PlayStoreApiBuilder();
        builder.setHttpClient(new OkHttpClientAdapter(context));
        builder.setDeviceInfoProvider(getDeviceInfoProvider(context));
        builder.setLocale(loginInfo.getLocale());
        builder.setEmail(loginInfo.getEmail());
        builder.setPassword(loginInfo.getPassword());
        builder.setGsfId(loginInfo.getGsfId());
        builder.setToken(loginInfo.getToken());
        builder.setDeviceCheckinConsistencyToken(loginInfo.getDeviceCheckinConsistencyToken());
        builder.setDeviceConfigToken(loginInfo.getDeviceConfigToken());
        builder.setDfeCookie(loginInfo.getDfeCookie());
        builder.setLoginToken(loginInfo.getLoginToken());
        builder.setLoginCaptcha(loginInfo.getLoginCaptcha());
        return builder;
    }

    private static DeviceInfoProvider getDeviceInfoProvider(Context context) {
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

    private static void saveLoginInfo(Context context, LoginInfo loginInfo) {
        Util.getPrefs(context)
                .edit()
                .putString(Accountant.ACCOUNT_EMAIL, loginInfo.getEmail())
                .putString(Accountant.GSF_ID, loginInfo.getGsfId())
                .putString(Accountant.AUTH_TOKEN, loginInfo.getToken())
                .apply();
    }
}
