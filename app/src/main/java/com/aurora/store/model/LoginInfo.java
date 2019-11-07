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

package com.aurora.store.model;

import android.content.Context;
import android.text.TextUtils;

import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.PrefUtil;
import com.google.gson.Gson;

import java.util.Locale;

import lombok.Data;

@Data
public class LoginInfo {

    private String email;
    private String userName;
    private String userPicUrl;
    private String aasToken;
    private String gsfId;
    private String authToken;
    private String locale;
    private String tokenDispenserUrl;
    private String deviceDefinitionName;
    private String deviceDefinitionDisplayName;
    private String deviceCheckinConsistencyToken;
    private String deviceConfigToken;
    private String dfeCookie;

    public static void save(Context context, LoginInfo loginInfo) {
        Gson gson = new Gson();
        String loginString = gson.toJson(loginInfo, LoginInfo.class);
        PrefUtil.putBoolean(context, Accountant.LOGGED_IN, true);
        PrefUtil.putString(context, Accountant.DATA, loginString);
    }

    public static LoginInfo getSavedInstance(Context context) {
        Gson gson = new Gson();
        String loginString = PrefUtil.getString(context, Accountant.DATA);
        LoginInfo loginInfo = gson.fromJson(loginString, LoginInfo.class);
        return loginInfo == null ? new LoginInfo() : loginInfo;
    }

    public static void removeSavedInstance(Context context) {
        PrefUtil.putString(context, Accountant.DATA, "");
    }

    public Locale getLocale() {
        return TextUtils.isEmpty(locale) ? Locale.getDefault() : new Locale(locale);
    }
}
