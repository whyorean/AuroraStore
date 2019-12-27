/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
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

package com.aurora.store.util;

import android.content.Context;

import com.aurora.store.model.LoginInfo;

public class Accountant {
    public static final String DATA = "DATA";
    public static final String EMAIL = "EMAIL";
    public static final String PROFILE_NAME = "PROFILE_NAME";
    public static final String PROFILE_AVATAR = "PROFILE_AVATAR";
    public static final String PROFILE_BACKGROUND = "PROFILE_BACKGROUND";
    public static final String LOGGED_IN = "LOGGED_IN";
    public static final String ANONYMOUS = "ANONYMOUS";

    public static Boolean isLoggedIn(Context context) {
        return PrefUtil.getBoolean(context, LOGGED_IN);
    }


    public static Boolean isAnonymous(Context context) {
        return PrefUtil.getBoolean(context, ANONYMOUS);
    }

    public static String getUserName(Context context) {
        return PrefUtil.getString(context, PROFILE_NAME);
    }

    public static String getEmail(Context context) {

        return PrefUtil.getString(context, EMAIL);
    }

    public static String getImageURL(Context context) {
        return PrefUtil.getString(context, PROFILE_AVATAR);
    }

    public static String getBackgroundImageURL(Context context) {
        return PrefUtil.getString(context, PROFILE_BACKGROUND);
    }

    public static void completeCheckout(Context context) {
        PrefUtil.remove(context, LOGGED_IN);
        PrefUtil.remove(context, EMAIL);
        PrefUtil.remove(context, PROFILE_NAME);
        PrefUtil.remove(context, PROFILE_AVATAR);
        PrefUtil.remove(context, PROFILE_BACKGROUND);
        LoginInfo.removeSavedInstance(context);
    }

    public static void setLoggedIn(Context context) {
        PrefUtil.putBoolean(context, LOGGED_IN, true);
    }

    public static void setAnonymous(Context context, boolean value) {
        PrefUtil.putBoolean(context, ANONYMOUS, value);
    }
}
