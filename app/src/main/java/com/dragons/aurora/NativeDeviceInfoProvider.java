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

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.dragons.aurora.playstoreapiv2.AndroidBuildProto;
import com.dragons.aurora.playstoreapiv2.AndroidCheckinProto;
import com.dragons.aurora.playstoreapiv2.AndroidCheckinRequest;
import com.dragons.aurora.playstoreapiv2.DeviceConfigurationProto;
import com.dragons.aurora.playstoreapiv2.DeviceInfoProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NativeDeviceInfoProvider implements DeviceInfoProvider {

    private Context context;
    private String localeString;
    private String networkOperator = "";
    private String simOperator = "";
    private NativeGsfVersionProvider gsfVersionProvider;

    static public List<String> getPlatforms() {
        List<String> platforms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            platforms = Arrays.asList(Build.SUPPORTED_ABIS);
        } else {
            if (!TextUtils.isEmpty(Build.CPU_ABI)) {
                platforms.add(Build.CPU_ABI);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO && !TextUtils.isEmpty(Build.CPU_ABI2)) {
                platforms.add(Build.CPU_ABI2);
            }
        }
        return platforms;
    }

    static public List<String> getFeatures(Context context) {
        List<String> featureStringList = new ArrayList<>();
        for (FeatureInfo feature : context.getPackageManager().getSystemAvailableFeatures()) {
            if (!TextUtils.isEmpty(feature.name)) {
                featureStringList.add(feature.name);
            }
        }
        Collections.sort(featureStringList);
        return featureStringList;
    }

    static public List<String> getLocales(Context context) {
        List<String> rawLocales = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            rawLocales.addAll(Arrays.asList(context.getAssets().getLocales()));
        } else {
            for (Locale locale : Locale.getAvailableLocales()) {
                rawLocales.add(locale.toString());
            }
        }
        List<String> locales = new ArrayList<>();
        for (String locale : rawLocales) {
            if (TextUtils.isEmpty(locale)) {
                continue;
            }
            locales.add(locale.replace("-", "_"));
        }
        Collections.sort(locales);
        return locales;
    }

    static public List<String> getSharedLibraries(Context context) {
        List<String> libraries = new ArrayList<>();
        libraries.addAll(Arrays.asList(context.getPackageManager().getSystemSharedLibraryNames()));
        Collections.sort(libraries);
        return libraries;
    }

    public void setContext(Context context) {
        this.context = context;
        gsfVersionProvider = new NativeGsfVersionProvider(context);
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (null != tm) {
            networkOperator = null != tm.getNetworkOperator() ? tm.getNetworkOperator() : "";
            simOperator = null != tm.getSimOperator() ? tm.getSimOperator() : "";
        }
    }

    public void setLocaleString(String localeString) {
        this.localeString = localeString;
    }

    public int getSdkVersion() {
        return Build.VERSION.SDK_INT;
    }

    public int getPlayServicesVersion() {
        return gsfVersionProvider.getGsfVersionCode(true);
    }

    public String getMccmnc() {
        return simOperator;
    }

    public String getAuthUserAgentString() {
        return "GoogleAuth/1.4 (" + Build.DEVICE + " " + Build.ID + ")";
    }

    public String getUserAgentString() {
        return "Android-Finsky/" + gsfVersionProvider.getVendingVersionString(true) + " ("
                + "api=3"
                + ",versionCode=" + gsfVersionProvider.getVendingVersionCode(true)
                + ",sdk=" + Build.VERSION.SDK_INT
                + ",device=" + Build.DEVICE
                + ",hardware=" + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? Build.HARDWARE : Build.PRODUCT)
                + ",product=" + Build.PRODUCT
                + ",platformVersionRelease=" + Build.VERSION.RELEASE
                + ",model=" + Build.MODEL
                + ",buildId=" + Build.ID
                + ",isWideScreen=" + (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? "1" : "0")
                + ",supportedAbis=" + TextUtils.join(";", getPlatforms())
                + ")"
                ;
    }

    public AndroidCheckinRequest generateAndroidCheckinRequest() {
        return AndroidCheckinRequest
                .newBuilder()
                .setId(0)
                .setCheckin(getCheckinProto())
                .setLocale(this.localeString)
                .setTimeZone(TimeZone.getDefault().getID())
                .setVersion(3)
                .setDeviceConfiguration(getDeviceConfigurationProto())
                .setFragment(0)
                .build()
                ;
    }

    private AndroidCheckinProto getCheckinProto() {
        return AndroidCheckinProto.newBuilder()
                .setBuild(getBuildProto())
                .setLastCheckinMsec(0)
                .setCellOperator(networkOperator)
                .setSimOperator(simOperator)
                .setRoaming("mobile-notroaming")
                .setUserNumber(0)
                .build()
                ;
    }

    private AndroidBuildProto getBuildProto() {
        return AndroidBuildProto.newBuilder()
                .setId(Build.FINGERPRINT)
                .setProduct(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? Build.HARDWARE : Build.PRODUCT)
                .setCarrier(Build.BRAND)
                .setRadio(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? Build.RADIO : Build.MODEL)
                .setBootloader(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? Build.BOOTLOADER : Build.MODEL)
                .setDevice(Build.DEVICE)
                .setSdkVersion(Build.VERSION.SDK_INT)
                .setModel(Build.MODEL)
                .setManufacturer(Build.MANUFACTURER)
                .setBuildProduct(Build.PRODUCT)
                .setClient("android-google")
                .setOtaInstalled(false)
                .setTimestamp(System.currentTimeMillis() / 1000)
                .setGoogleServices(getPlayServicesVersion())
                .build()
                ;
    }

    public DeviceConfigurationProto getDeviceConfigurationProto() {
        DeviceConfigurationProto.Builder builder = DeviceConfigurationProto.newBuilder();
        addDisplayMetrics(builder);
        addConfiguration(builder);
        return builder
                .addAllNativePlatform(getPlatforms())
                .addAllSystemSharedLibrary(getSharedLibraries(context))
                .addAllSystemAvailableFeature(getFeatures(context))
                .addAllSystemSupportedLocale(getLocales(context))
                .setGlEsVersion(((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getDeviceConfigurationInfo().reqGlEsVersion)
                .addAllGlExtension(EglExtensionRetriever.getEglExtensions())
                .build()
                ;
    }

    private DeviceConfigurationProto.Builder addDisplayMetrics(DeviceConfigurationProto.Builder builder) {
        DisplayMetrics metrics = this.context.getResources().getDisplayMetrics();
        builder
                .setScreenDensity((int) (metrics.density * 160f))
                .setScreenWidth(metrics.widthPixels)
                .setScreenHeight(metrics.heightPixels)
        ;
        return builder;
    }

    private DeviceConfigurationProto.Builder addConfiguration(DeviceConfigurationProto.Builder builder) {
        Configuration config = this.context.getResources().getConfiguration();
        builder
                .setTouchScreen(config.touchscreen)
                .setKeyboard(config.keyboard)
                .setNavigation(config.navigation)
                .setScreenLayout(config.screenLayout & 15)
                .setHasHardKeyboard(config.keyboard == Configuration.KEYBOARD_QWERTY)
                .setHasFiveWayNavigation(config.navigation == Configuration.NAVIGATIONHIDDEN_YES)
        ;
        return builder;
    }
}
