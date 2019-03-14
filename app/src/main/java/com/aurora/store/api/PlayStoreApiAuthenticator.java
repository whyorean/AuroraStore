/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.aurora.store.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.aurora.store.utility.Accountant;
import com.aurora.store.Constants;
import com.aurora.store.manager.SpoofManager;
import com.aurora.store.adapter.OkHttpClientAdapter;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.model.LoginInfo;
import com.aurora.store.provider.NativeDeviceInfoProvider;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;
import com.dragons.aurora.playstoreapiv2.ApiBuilderException;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.DeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.PropertiesDeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.TokenDispenserException;

import java.io.IOException;
import java.util.Locale;

public class PlayStoreApiAuthenticator {

    private static final int RETRIES = 5;
    private static final String tokenDispenserURL = "http://www.auroraoss.com:8080";
    private static GooglePlayAPI api;

    private Context context;
    private SharedPreferences prefs;

    public PlayStoreApiAuthenticator(Context context) {
        this.context = context;
    }

    public GooglePlayAPI getApi() throws IOException {
        if (api == null) {
            api = buildFromPreferences();
        }
        return api;
    }

    public boolean login() throws IOException {
        LoginInfo loginInfo = new LoginInfo();
        api = build(loginInfo);
        Util
                .getPrefs(context.getApplicationContext())
                .edit()
                .putBoolean(Accountant.DUMMY_ACCOUNT, true)
                .putString(Accountant.LAST_USED_TOKEN_DISPENSER, loginInfo.getTokenDispenserUrl())
                .apply();
        return api != null;
    }

    public boolean login(String email, String password) throws IOException {
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setEmail(email);
        loginInfo.setPassword(password);
        api = build(loginInfo);
        Util
                .getPrefs(context.getApplicationContext())
                .edit()
                .remove(Accountant.DUMMY_ACCOUNT)
                .apply();
        return api != null;
    }

    public boolean refreshToken() throws IOException {
        prefs = Util.getPrefs(context.getApplicationContext());
        prefs
                .edit()
                .remove(Accountant.AUTH_TOKEN)
                .apply();
        String email = prefs.getString(Accountant.ACCOUNT_EMAIL, "");
        if (TextUtils.isEmpty(email)) {
            throw new CredentialsEmptyException();
        }
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setEmail(email);
        loginInfo.setTokenDispenserUrl(prefs.getString(Accountant.LAST_USED_TOKEN_DISPENSER, ""));
        api = build(loginInfo);
        prefs
                .edit()
                .putBoolean(Accountant.DUMMY_ACCOUNT, true)
                .putString(Accountant.LAST_USED_TOKEN_DISPENSER, loginInfo.getTokenDispenserUrl())
                .apply();
        return api != null;
    }

    public void logout() {
        Util.getPrefs(context)
                .edit()
                .remove(Accountant.ACCOUNT_EMAIL)
                .remove(Accountant.GSF_ID)
                .remove(Accountant.AUTH_TOKEN)
                .remove(Accountant.LAST_USED_TOKEN_DISPENSER)
                .remove(Accountant.DUMMY_ACCOUNT)
                .apply();
        api = null;
    }

    private GooglePlayAPI buildFromPreferences() throws IOException {
        prefs = Util.getPrefs(context.getApplicationContext());
        String email = prefs.getString(Accountant.ACCOUNT_EMAIL, "");
        if (TextUtils.isEmpty(email)) {
            throw new CredentialsEmptyException();
        }
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setEmail(email);
        return build(loginInfo);
    }

    private GooglePlayAPI build(LoginInfo loginInfo) throws IOException {
        api = build(loginInfo, RETRIES);
        loginInfo.setGsfId(api.getGsfId());
        loginInfo.setToken(api.getToken());
        save(loginInfo);
        return api;
    }

    private GooglePlayAPI build(LoginInfo loginInfo, int retries) throws IOException {
        int tried = 0;
        while (tried < retries) {
            try {
                com.dragons.aurora.playstoreapiv2.PlayStoreApiBuilder builder = getBuilder(loginInfo);
                GooglePlayAPI api = builder.build();
                loginInfo.setEmail(builder.getEmail());
                return api;
            } catch (ApiBuilderException e) {
                throw new RuntimeException(e);
            } catch (AuthException | TokenDispenserException e) {
                if (Util.noNetwork(e.getCause())) {
                    throw (IOException) e.getCause();
                }
                loginInfo.setTokenDispenserUrl(null);
                prefs = Util.getPrefs(context.getApplicationContext());
                if (prefs.getBoolean(Accountant.DUMMY_ACCOUNT, false)) {
                    loginInfo.setEmail(null);
                    prefs.edit().remove(Accountant.GSF_ID).apply();
                }
                tried++;
                if (tried >= retries) {
                    throw e;
                }
                Log.i(Constants.TAG, "Login retry : " + tried);
            }
        }
        return null;
    }

    private com.dragons.aurora.playstoreapiv2.PlayStoreApiBuilder getBuilder(LoginInfo loginInfo) {
        fill(loginInfo);
        return new com.dragons.aurora.playstoreapiv2.PlayStoreApiBuilder()
                .setHttpClient(new OkHttpClientAdapter())
                .setDeviceInfoProvider(getDeviceInfoProvider())
                .setLocale(loginInfo.getLocale())
                .setEmail(loginInfo.getEmail())
                .setPassword(loginInfo.getPassword())
                .setGsfId(loginInfo.getGsfId())
                .setToken(loginInfo.getToken())
                .setTokenDispenserUrl(loginInfo.getTokenDispenserUrl());
    }

    private DeviceInfoProvider getDeviceInfoProvider() {
        DeviceInfoProvider deviceInfoProvider;
        String spoofDevice = PrefUtil.getString(context, Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE);
        if (TextUtils.isEmpty(spoofDevice)) {
            deviceInfoProvider = new NativeDeviceInfoProvider();
            ((NativeDeviceInfoProvider) deviceInfoProvider).setContext(context);
            ((NativeDeviceInfoProvider) deviceInfoProvider).setLocaleString(Locale.getDefault().toString());
        } else {
            deviceInfoProvider = new PropertiesDeviceInfoProvider();
            ((PropertiesDeviceInfoProvider) deviceInfoProvider).setProperties(new SpoofManager(context).getProperties(spoofDevice));
            ((PropertiesDeviceInfoProvider) deviceInfoProvider).setLocaleString(Locale.getDefault().toString());
        }
        return deviceInfoProvider;
    }

    private void fill(LoginInfo loginInfo) {
        prefs = Util.getPrefs(context.getApplicationContext());
        String locale = prefs.getString(Constants.PREFERENCE_REQUESTED_LANGUAGE, "");
        loginInfo.setLocale(TextUtils.isEmpty(locale) ? Locale.getDefault() : new Locale(locale));
        loginInfo.setGsfId(prefs.getString(Accountant.GSF_ID, ""));
        loginInfo.setToken(prefs.getString(Accountant.AUTH_TOKEN, ""));
        if (TextUtils.isEmpty(loginInfo.getTokenDispenserUrl())) {
            loginInfo.setTokenDispenserUrl(tokenDispenserURL);
        }
    }

    private void save(LoginInfo loginInfo) {
        Util.getPrefs(context)
                .edit()
                .putString(Accountant.ACCOUNT_EMAIL, loginInfo.getEmail())
                .putString(Accountant.GSF_ID, loginInfo.getGsfId())
                .putString(Accountant.AUTH_TOKEN, loginInfo.getToken())
                .apply();
    }
}
