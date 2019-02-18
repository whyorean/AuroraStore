package com.aurora.store.provider;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class NativeGsfVersionProvider {

    static private final String GOOGLE_SERVICES_PACKAGE_ID = "com.google.android.gms";
    static private final String GOOGLE_VENDING_PACKAGE_ID = "com.android.vending";

    static private final int GOOGLE_SERVICES_VERSION_CODE = 10548448;
    static private final int GOOGLE_VENDING_VERSION_CODE = 80798000;
    static private final String GOOGLE_VENDING_VERSION_STRING = "7.9.80";

    private int gsfVersionCode = 0;
    private int vendingVersionCode = 0;
    private String vendingVersionString = "";

    public NativeGsfVersionProvider(Context context) {
        try {
            gsfVersionCode = context.getPackageManager().getPackageInfo(GOOGLE_SERVICES_PACKAGE_ID, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // com.google.android.gms not found
        }
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(GOOGLE_VENDING_PACKAGE_ID, 0);
            vendingVersionCode = pi.versionCode;
            vendingVersionString = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // com.android.vending not found
        }
    }

    public int getGsfVersionCode(boolean defaultIfNotFound) {
        return defaultIfNotFound && gsfVersionCode < GOOGLE_SERVICES_VERSION_CODE
                ? GOOGLE_SERVICES_VERSION_CODE
                : gsfVersionCode
                ;
    }

    public int getVendingVersionCode(boolean defaultIfNotFound) {
        return defaultIfNotFound && vendingVersionCode < GOOGLE_VENDING_VERSION_CODE
                ? GOOGLE_VENDING_VERSION_CODE
                : vendingVersionCode
                ;
    }

    public String getVendingVersionString(boolean defaultIfNotFound) {
        return defaultIfNotFound && vendingVersionCode < GOOGLE_VENDING_VERSION_CODE
                ? GOOGLE_VENDING_VERSION_STRING
                : vendingVersionString
                ;
    }
}
