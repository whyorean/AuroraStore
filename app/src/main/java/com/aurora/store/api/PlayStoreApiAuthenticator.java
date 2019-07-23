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
import android.widget.Toast;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.TokenDispenserMirrors;
import com.aurora.store.adapter.OkHttpClientAdapter;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.exception.TokenizerException;
import com.aurora.store.manager.LocaleManager;
import com.aurora.store.manager.SpoofManager;
import com.aurora.store.model.LoginInfo;
import com.aurora.store.provider.NativeDeviceInfoProvider;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;
import com.dragons.aurora.playstoreapiv2.ApiBuilderException;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.DeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.PlayStoreApiBuilder;
import com.dragons.aurora.playstoreapiv2.PropertiesDeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.TokenDispenserException;

import java.io.IOException;
import java.util.Locale;

public class PlayStoreApiAuthenticator {

    private static final int RETRIES = 5;
    private static TokenDispenserMirrors tokenDispenserMirrors = new TokenDispenserMirrors();
    private volatile static GooglePlayAPI api;

    private Context context;
    private SharedPreferences prefs;

    public PlayStoreApiAuthenticator(Context context) {
        this.context = context;
        prefs = Util.getPrefs(context);
    }

    public synchronized GooglePlayAPI getApi() throws IOException {
        if (api == null) {
            api = buildFromPreferences();
        }
        return api;
    }

    public boolean login() throws IOException {
        LoginInfo loginInfo = new LoginInfo();
        api = build(loginInfo);
        Util
                .getPrefs(context.getApplicationContext()).edit()
                .putBoolean(Accountant.DUMMY_ACCOUNT, true)
                .putString(Accountant.ACCOUNT_EMAIL, loginInfo.getEmail())
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
        String email = prefs.getString(Accountant.ACCOUNT_EMAIL, "");
        if (TextUtils.isEmpty(email)) {
            throw new CredentialsEmptyException();
        }
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setEmail(email);
        return build(loginInfo);
    }

    private GooglePlayAPI build(LoginInfo loginInfo) throws IOException {
        GooglePlayAPI api = build(loginInfo, RETRIES);
        if (api != null) {
            loginInfo.setGsfId(api.getGsfId());
            loginInfo.setToken(api.getToken());
            loginInfo.setDfeCookie(api.getDfeCookie());
            loginInfo.setDeviceConfigToken(api.getDeviceConfigToken());
            loginInfo.setDeviceCheckinConsistencyToken(api.getDeviceCheckinConsistencyToken());
            saveLoginInfo(loginInfo);
        }
        return api;
    }

    private GooglePlayAPI build(LoginInfo loginInfo, int retries) throws IOException {
        int tried = 0;
        while (tried < retries) {
            try {
                PlayStoreApiBuilder builder = getBuilder(loginInfo);
                GooglePlayAPI api = builder.build();
                loginInfo.setEmail(builder.getEmail());
                return api;
            } catch (ApiBuilderException e) {
                throw new RuntimeException(e);
            } catch (AuthException | TokenDispenserException e) {
                if (Util.noNetwork(e.getCause())) {
                    ContextUtil.runOnUiThread(() -> Toast.makeText(context,
                            context.getString(R.string.error_no_network),
                            Toast.LENGTH_LONG).show());
                    throw (IOException) e.getCause();
                }
                if (loginInfo.appProvidedEmail()) {
                    loginInfo.setTokenDispenserUrl(tokenDispenserMirrors.get(context));
                    loginInfo.setEmail(null);
                    loginInfo.setGsfId(null);
                }
                tried++;
                if (tried >= retries) {
                    throw new TokenizerException("Anonymous login failed, try again later.", e.getCause());
                }
                Log.i("Anonymous Login Failed @ %s, attempt %d",
                        loginInfo.getTokenDispenserUrl(), tried);
            }
        }
        return null;
    }

    private PlayStoreApiBuilder getBuilder(LoginInfo loginInfo) {
        String locale = prefs.getString(Constants.PREFERENCE_REQUESTED_LANGUAGE, "");
        loginInfo.setLocale(TextUtils.isEmpty(locale) ? Locale.getDefault().toString() : locale);
        loginInfo.setGsfId(prefs.getString(Accountant.GSF_ID, ""));
        loginInfo.setToken(prefs.getString(Accountant.AUTH_TOKEN, ""));
        loginInfo.setTokenDispenserUrl(tokenDispenserMirrors.get(context));

        return new PlayStoreApiBuilder()
                .setHttpClient(new OkHttpClientAdapter(context))
                .setDeviceInfoProvider(getDeviceInfoProvider())
                .setLocale(loginInfo.getLocale())
                .setEmail(loginInfo.getEmail())
                .setPassword(loginInfo.getPassword())
                .setGsfId(loginInfo.getGsfId())
                .setToken(loginInfo.getToken())
                .setTokenDispenserUrl(loginInfo.getTokenDispenserUrl())
                .setDeviceCheckinConsistencyToken(loginInfo.getDeviceCheckinConsistencyToken())
                .setDeviceConfigToken(loginInfo.getDeviceConfigToken())
                .setDfeCookie(loginInfo.getDfeCookie());
    }

    private DeviceInfoProvider getDeviceInfoProvider() {
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

    private void saveLoginInfo(LoginInfo loginInfo) {
        Util.getPrefs(context)
                .edit()
                .putString(Accountant.ACCOUNT_EMAIL, loginInfo.getEmail())
                .putString(Accountant.GSF_ID, loginInfo.getGsfId())
                .putString(Accountant.AUTH_TOKEN, loginInfo.getToken())
                .putString(Accountant.LAST_USED_TOKEN_DISPENSER, loginInfo.getTokenDispenserUrl())
                .apply();
    }
}
