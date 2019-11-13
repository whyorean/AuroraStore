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

import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import lombok.Data;

@Data
public class LoginInfo implements Comparable<LoginInfo> {

    private String email;
    private String userName;
    private String userPicUrl;
    private String password;
    private String gsfId;
    private String token;
    private String locale;
    private String tokenDispenserUrl;
    private String deviceDefinitionName;
    private String deviceDefinitionDisplayName;
    private String deviceCheckinConsistencyToken;
    private String deviceConfigToken;
    private String dfeCookie;
    private String loginToken;
    private String loginCaptcha;
    private String captchaUrl;

    public Locale getLocale() {
        return TextUtils.isEmpty(locale) ? Locale.getDefault() : new Locale(locale);
    }

    public boolean appProvidedEmail() {
        return !TextUtils.isEmpty(tokenDispenserUrl);
    }

    private boolean isLoggedIn() {
        return !TextUtils.isEmpty(email) && !TextUtils.isEmpty(gsfId);
    }

    public void clear() {
        email = null;
        userName = null;
        userPicUrl = null;
        password = null;
        gsfId = null;
        token = null;
        locale = null;
        tokenDispenserUrl = null;
        deviceDefinitionName = null;
        deviceDefinitionDisplayName = null;
        deviceCheckinConsistencyToken = null;
        deviceConfigToken = null;
        dfeCookie = null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LoginInfo
                && isLoggedIn()
                && ((LoginInfo) obj).isLoggedIn()
                && !TextUtils.isEmpty(deviceDefinitionName)
                && !TextUtils.isEmpty(((LoginInfo) obj).getDeviceDefinitionName())
                && deviceDefinitionName.equals(((LoginInfo) obj).getDeviceDefinitionName())
                ;
    }

    @Override
    public int hashCode() {
        return TextUtils.isEmpty(email)
                ? 0
                : ((appProvidedEmail() ? "" : email) + "|" + deviceDefinitionName).hashCode();
    }

    @Override
    public int compareTo(@NotNull LoginInfo loginInfo) {
        if (TextUtils.isEmpty(getUserName())
                || TextUtils.isEmpty(loginInfo.getUserName())
                || TextUtils.isEmpty(getDeviceDefinitionName())
                || TextUtils.isEmpty(loginInfo.getDeviceDefinitionName())) {
            return 0;
        }
        int result = getUserName().compareTo(loginInfo.getUserName());
        return result == 0
                ? getDeviceDefinitionName().compareTo(loginInfo.getDeviceDefinitionName())
                : result;
    }
}
