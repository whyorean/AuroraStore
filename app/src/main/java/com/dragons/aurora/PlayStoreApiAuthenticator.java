/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
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
 */

package com.dragons.aurora;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.dragons.aurora.adapters.NativeHttpClientAdapter;
import com.dragons.aurora.model.LoginInfo;
import com.dragons.aurora.playstoreapiv2.ApiBuilderException;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.DeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.PropertiesDeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.TokenDispenserException;
import com.dragons.aurora.task.playstore.PlayStoreTask;

import java.io.IOException;
import java.util.Locale;

import timber.log.Timber;

public class PlayStoreApiAuthenticator {

    static private final int RETRIES = 5;
    private static GooglePlayAPI api;
    private static TokenDispenserMirrors tokenDispenserMirrors = new TokenDispenserMirrors();
    private Context context;

    public PlayStoreApiAuthenticator(Context context) {
        this.context = context;
    }

    public GooglePlayAPI getApi() throws IOException {
        if (api == null) {
            api = buildFromPreferences();
        }
        return api;
    }

    public void login() throws IOException {
        LoginInfo loginInfo = new LoginInfo();
        api = build(loginInfo);
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(Aurora.PREFERENCE_APP_PROVIDED_EMAIL, true)
                .putString(Aurora.PREFERENCE_LAST_USED_TOKEN_DISPENSER, loginInfo.getTokenDispenserUrl())
                .apply()
        ;
    }

    public void login(String email, String password) throws IOException {
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setEmail(email);
        loginInfo.setPassword(password);
        api = build(loginInfo);
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(Aurora.PREFERENCE_APP_PROVIDED_EMAIL).apply();
    }

    public void refreshToken() throws IOException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(Aurora.PREFERENCE_AUTH_TOKEN).apply();
        String email = prefs.getString(Aurora.PREFERENCE_EMAIL, "");
        if (TextUtils.isEmpty(email)) {
            throw new CredentialsEmptyException();
        }
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setEmail(email);
        loginInfo.setTokenDispenserUrl(prefs.getString(Aurora.PREFERENCE_LAST_USED_TOKEN_DISPENSER, ""));
        api = build(loginInfo);
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(Aurora.PREFERENCE_APP_PROVIDED_EMAIL, true)
                .putString(Aurora.PREFERENCE_LAST_USED_TOKEN_DISPENSER, loginInfo.getTokenDispenserUrl())
                .apply()
        ;
    }

    public void logout() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(Aurora.PREFERENCE_EMAIL)
                .remove(Aurora.PREFERENCE_GSF_ID)
                .remove(Aurora.PREFERENCE_AUTH_TOKEN)
                .remove(Aurora.PREFERENCE_LAST_USED_TOKEN_DISPENSER)
                .remove(Aurora.PREFERENCE_APP_PROVIDED_EMAIL)
                .apply();
        api = null;
    }

    private GooglePlayAPI buildFromPreferences() throws IOException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String email = prefs.getString(Aurora.PREFERENCE_EMAIL, "");
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
        tokenDispenserMirrors.reset();
        while (tried < retries) {
            try {
                com.dragons.aurora.playstoreapiv2.PlayStoreApiBuilder builder = getBuilder(loginInfo);
                GooglePlayAPI api = builder.build();
                loginInfo.setEmail(builder.getEmail());
                return api;
            } catch (ApiBuilderException e) {
                // Impossible, unless there are mistakes, so no need to make it a declared exception
                throw new RuntimeException(e);
            } catch (AuthException | TokenDispenserException e) {
                if (PlayStoreTask.noNetwork(e.getCause())) {
                    throw (IOException) e.getCause();
                }
                loginInfo.setTokenDispenserUrl(null);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (prefs.getBoolean(Aurora.PREFERENCE_APP_PROVIDED_EMAIL, false)) {
                    loginInfo.setEmail(null);
                    prefs.edit().remove(Aurora.PREFERENCE_GSF_ID).apply();
                }
                tried++;
                if (tried >= retries) {
                    throw e;
                }
                Timber.i("Login retry : %s", tried);
            }
        }
        return null;
    }

    private com.dragons.aurora.playstoreapiv2.PlayStoreApiBuilder getBuilder(LoginInfo loginInfo) {
        fill(loginInfo);
        return new com.dragons.aurora.playstoreapiv2.PlayStoreApiBuilder()
                .setHttpClient(new NativeHttpClientAdapter())
                .setDeviceInfoProvider(getDeviceInfoProvider())
                .setLocale(loginInfo.getLocale())
                .setEmail(loginInfo.getEmail())
                .setPassword(loginInfo.getPassword())
                .setGsfId(loginInfo.getGsfId())
                .setToken(loginInfo.getToken())
                .setTokenDispenserUrl(loginInfo.getTokenDispenserUrl())
                ;
    }

    private DeviceInfoProvider getDeviceInfoProvider() {
        DeviceInfoProvider deviceInfoProvider;
        String spoofDevice = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Aurora.PREFERENCE_DEVICE_TO_PRETEND_TO_BE, "");
        if (TextUtils.isEmpty(spoofDevice)) {
            deviceInfoProvider = new NativeDeviceInfoProvider();
            ((NativeDeviceInfoProvider) deviceInfoProvider).setContext(context);
            ((NativeDeviceInfoProvider) deviceInfoProvider).setLocaleString(Locale.getDefault().toString());
        } else {
            deviceInfoProvider = new PropertiesDeviceInfoProvider();
            ((PropertiesDeviceInfoProvider) deviceInfoProvider).setProperties(new SpoofDeviceManager(context).getProperties(spoofDevice));
            ((PropertiesDeviceInfoProvider) deviceInfoProvider).setLocaleString(Locale.getDefault().toString());
        }
        return deviceInfoProvider;
    }

    private void fill(LoginInfo loginInfo) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String locale = prefs.getString(Aurora.PREFERENCE_REQUESTED_LANGUAGE, "");
        loginInfo.setLocale(TextUtils.isEmpty(locale) ? Locale.getDefault() : new Locale(locale));
        loginInfo.setGsfId(prefs.getString(Aurora.PREFERENCE_GSF_ID, ""));
        loginInfo.setToken(prefs.getString(Aurora.PREFERENCE_AUTH_TOKEN, ""));
        if (TextUtils.isEmpty(loginInfo.getTokenDispenserUrl())) {
            loginInfo.setTokenDispenserUrl(tokenDispenserMirrors.get());
        }
    }

    private void save(LoginInfo loginInfo) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(Aurora.PREFERENCE_EMAIL, loginInfo.getEmail())
                .putString(Aurora.PREFERENCE_GSF_ID, loginInfo.getGsfId())
                .putString(Aurora.PREFERENCE_AUTH_TOKEN, loginInfo.getToken())
                .apply()
        ;
    }
}
