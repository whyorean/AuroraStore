package com.aurora.store.utility;

import android.content.Context;

import com.aurora.store.api.PlayStoreApiAuthenticator;

public class Accountant {

    public static final String AUTH_TOKEN = "AUTH_TOKEN";
    public static final String DUMMY_ACCOUNT = "DUMMY_ACCOUNT";
    public static final String ACCOUNT_EMAIL = "ACCOUNT_EMAIL";
    public static final String GOOGLE_ACCOUNT = "GOOGLE_ACCOUNT";
    public static final String GOOGLE_NAME = "GOOGLE_NAME";
    public static final String GOOGLE_URL = "GOOGLE_URL";
    public static final String GSF_ID = "GSF_ID";
    public static final String LAST_USED_TOKEN_DISPENSER = "LAST_USED_TOKEN_DISPENSER";
    public static final String LOGGED_IN = "LOGGED_IN";
    public static final String LOGIN_PROMPTED = "LOGIN_PROMPTED";

    public static Boolean isGoogle(Context context) {
        return PrefUtil.getBoolean(context, GOOGLE_ACCOUNT);
    }

    public static Boolean isDummy(Context context) {
        return PrefUtil.getBoolean(context, DUMMY_ACCOUNT);
    }

    public static Boolean isLoggedIn(Context context) {
        return PrefUtil.getBoolean(context, LOGGED_IN);
    }

    public static String getUserName(Context context) {
        if (isDummy(context))
            return "Aurora OSS";
        else
            return PrefUtil.getString(context, GOOGLE_NAME);
    }

    public static String getEmail(Context context) {
        return PrefUtil.getString(context, ACCOUNT_EMAIL);
    }

    public static String getImageURL(Context context) {
        return PrefUtil.getString(context, GOOGLE_URL);
    }

    public static void completeCheckout(Context context) {
        PrefUtil.putBoolean(context, LOGGED_IN, false);
        PrefUtil.putString(context, GOOGLE_NAME, "");
        PrefUtil.putString(context, GOOGLE_URL, "");
        PrefUtil.putBoolean(context, LOGIN_PROMPTED, false);
        new PlayStoreApiAuthenticator(context).logout();
    }

    public static void saveDummy(Context context) {
        PrefUtil.putBoolean(context, LOGGED_IN, true);
        PrefUtil.putBoolean(context, DUMMY_ACCOUNT, true);
        PrefUtil.putBoolean(context, GOOGLE_ACCOUNT, false);
    }

    public static void saveGoogle(Context context) {
        PrefUtil.putBoolean(context, LOGGED_IN, true);
        PrefUtil.putBoolean(context, DUMMY_ACCOUNT, false);
        PrefUtil.putBoolean(context, GOOGLE_ACCOUNT, true);
    }
}
