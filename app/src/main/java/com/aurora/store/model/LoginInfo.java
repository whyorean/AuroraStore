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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPicUrl() {
        return userPicUrl;
    }

    public void setUserPicUrl(String userPicUrl) {
        this.userPicUrl = userPicUrl;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGsfId() {
        return gsfId;
    }

    public void setGsfId(String gsfId) {
        this.gsfId = gsfId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Locale getLocale() {
        return TextUtils.isEmpty(locale) ? Locale.getDefault() : new Locale(locale);
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getLocaleString() {
        return locale;
    }

    public String getTokenDispenserUrl() {
        return tokenDispenserUrl;
    }

    public void setTokenDispenserUrl(String tokenDispenserUrl) {
        this.tokenDispenserUrl = tokenDispenserUrl;
    }

    public String getDeviceDefinitionName() {
        return deviceDefinitionName;
    }

    public void setDeviceDefinitionName(String deviceDefinitionName) {
        this.deviceDefinitionName = deviceDefinitionName;
    }

    public String getDeviceDefinitionDisplayName() {
        return deviceDefinitionDisplayName;
    }

    public void setDeviceDefinitionDisplayName(String deviceDefinitionDisplayName) {
        this.deviceDefinitionDisplayName = deviceDefinitionDisplayName;
    }

    public String getDeviceCheckinConsistencyToken() {
        return deviceCheckinConsistencyToken;
    }

    public void setDeviceCheckinConsistencyToken(String deviceCheckinConsistencyToken) {
        this.deviceCheckinConsistencyToken = deviceCheckinConsistencyToken;
    }

    public String getDeviceConfigToken() {
        return deviceConfigToken;
    }

    public void setDeviceConfigToken(String deviceConfigToken) {
        this.deviceConfigToken = deviceConfigToken;
    }

    public String getDfeCookie() {
        return dfeCookie;
    }

    public void setDfeCookie(String dfeCookie) {
        this.dfeCookie = dfeCookie;
    }

    public boolean appProvidedEmail() {
        return !TextUtils.isEmpty(tokenDispenserUrl);
    }

    public boolean isLoggedIn() {
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
                : ((appProvidedEmail() ? "" : email) + "|" + deviceDefinitionName).hashCode()
                ;
    }

    @Override
    public int compareTo(LoginInfo o) {
        if (TextUtils.isEmpty(getUserName())
                || TextUtils.isEmpty(o.getUserName())
                || TextUtils.isEmpty(getDeviceDefinitionName())
                || TextUtils.isEmpty(o.getDeviceDefinitionName())
        ) {
            return 0;
        }
        int result = getUserName().compareTo(o.getUserName());
        return result == 0
                ? getDeviceDefinitionName().compareTo(o.getDeviceDefinitionName())
                : result
                ;
    }

    @Override
    public String toString() {
        List<String> fieldStrings = new ArrayList<>();
        for (Field field : getClass().getDeclaredFields()) {
            try {
                fieldStrings.add(field.getName() + "=" + field.get(this));
            } catch (IllegalAccessException e) {
                fieldStrings.add(field.getName() + "=[inaccessible]");
            }
        }
        return getClass().getSimpleName() + " hashCode=" + hashCode() + ", " + TextUtils.join(", ", fieldStrings);
    }
}
