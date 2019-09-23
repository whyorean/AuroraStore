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
import android.text.TextUtils;

import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.model.LoginInfo;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ApiBuilderUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;

public class PlayStoreApiAuthenticator {

    private static volatile PlayStoreApiAuthenticator instance;
    private static volatile GooglePlayAPI api;

    public PlayStoreApiAuthenticator() {
        if (instance != null) {
            throw new RuntimeException("Use getApi() method to get the single instance of RxBus");
        }
    }

    public static GooglePlayAPI getApi(Context context) {
        if (instance == null) {
            synchronized (PlayStoreApiAuthenticator.class) {
                if (instance == null) {
                    instance = new PlayStoreApiAuthenticator();
                    api = instance.buildApi(context);
                }
            }
        }
        return api;
    }

    public static boolean login(Context context) throws IOException {
        LoginInfo loginInfo = new LoginInfo();
        api = ApiBuilderUtil.build(context, loginInfo);
        Util
                .getPrefs(context.getApplicationContext()).edit()
                .putBoolean(Accountant.DUMMY_ACCOUNT, true)
                .putString(Accountant.ACCOUNT_EMAIL, loginInfo.getEmail())
                .putString(Accountant.LAST_USED_TOKEN_DISPENSER, loginInfo.getTokenDispenserUrl())
                .apply();
        return api != null;
    }

    public static boolean login(Context context, String email, String password) throws IOException {
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setEmail(email);
        loginInfo.setPassword(password);
        GooglePlayAPI api = ApiBuilderUtil.build(context, loginInfo);
        PrefUtil.remove(context, Accountant.DUMMY_ACCOUNT);
        return api != null;
    }

    public static boolean refreshToken(Context context) throws IOException {
        PrefUtil.remove(context, Accountant.AUTH_TOKEN);
        String email = PrefUtil.getString(context, Accountant.ACCOUNT_EMAIL);
        if (TextUtils.isEmpty(email)) {
            throw new CredentialsEmptyException();
        }
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setEmail(email);
        loginInfo.setTokenDispenserUrl(PrefUtil.getString(context, Accountant.LAST_USED_TOKEN_DISPENSER));
        GooglePlayAPI api = ApiBuilderUtil.build(context, loginInfo);
        PrefUtil.putBoolean(context, Accountant.DUMMY_ACCOUNT, true);
        PrefUtil.putString(context, Accountant.LAST_USED_TOKEN_DISPENSER, loginInfo.getTokenDispenserUrl());
        return api != null;
    }

    public static void logout(Context context) {
        PrefUtil.remove(context, Accountant.ACCOUNT_EMAIL);
        PrefUtil.remove(context, Accountant.GSF_ID);
        PrefUtil.remove(context, Accountant.AUTH_TOKEN);
        PrefUtil.remove(context, (Accountant.LAST_USED_TOKEN_DISPENSER));
        PrefUtil.remove(context, Accountant.DUMMY_ACCOUNT);
        instance = null;
    }

    private synchronized GooglePlayAPI buildApi(Context context) {
        if (api == null) {
            try {
                api = ApiBuilderUtil.buildFromPreferences(context);
            } catch (Exception e) {
                Log.e("Error building API -> %s", e.getMessage());
            }
        }
        return api;
    }
}
