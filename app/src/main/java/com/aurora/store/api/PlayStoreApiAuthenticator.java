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
import com.aurora.store.exception.InvalidApiException;
import com.aurora.store.model.LoginInfo;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ApiBuilderUtil;
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

    public static GooglePlayAPI getApi(Context context) throws Exception {
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

    public static boolean login(Context context, String email, String password) throws IOException {
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setEmail(email);
        loginInfo.setPassword(password);
        GooglePlayAPI api = ApiBuilderUtil.build2(context, loginInfo);
        return api != null;
    }

    public static void destroyInstance() {
        api = null;
        instance = null;
    }

    private synchronized GooglePlayAPI buildApi(Context context) throws Exception {
        if (api == null) {
            api = ApiBuilderUtil.buildFromPreferences(context);
        }
        return api;
    }
}
