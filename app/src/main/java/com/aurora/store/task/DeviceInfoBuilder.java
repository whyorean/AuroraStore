/*
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
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
 */

package com.aurora.store.task;

import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.aurora.store.provider.EglExtensionProvider;
import com.aurora.store.provider.NativeDeviceInfoProvider;
import com.aurora.store.provider.NativeGsfVersionProvider;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PathUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

public class DeviceInfoBuilder extends ContextWrapper {

    static private Map<String, String> staticProperties = new HashMap<>();

    static {
        staticProperties.put("Client", "android-google");
        staticProperties.put("Roaming", "mobile-notroaming");
        staticProperties.put("TimeZone", TimeZone.getDefault().getID());
        staticProperties.put("GL.Extensions", TextUtils.join(",", EglExtensionProvider.getEglExtensions()));
    }

    private Context context;

    public DeviceInfoBuilder(Context context) {
        super(context);
        this.context = context;
    }

    static String buildProperties(Map<String, String> properties) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String key : properties.keySet()) {
            stringBuilder
                    .append(key)
                    .append(" = ")
                    .append(properties.get(key))
                    .append("\n")
            ;
        }
        return stringBuilder.toString();
    }

    public boolean build() {
        final File file = new File(PathUtil.getExtBaseDirectory(context), "device-" + Build.DEVICE + ".properties");
        final String content = buildProperties(getDeviceInfo());
        try {
            Writer writer = new FileWriter(file);
            writer.write(content);
            writer.flush();
            return true;
        } catch (IOException e) {
            Log.e("Filed to write device info");
            return false;
        }
    }

    private Map<String, String> getDeviceInfo() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("UserReadableName", getUserReadableName());
        values.putAll(getBuildValues());
        values.putAll(getConfigurationValues());
        values.putAll(getDisplayMetricsValues());
        values.putAll(getPackageManagerValues());
        values.putAll(getOperatorValues());
        values.putAll(staticProperties);
        return values;
    }

    private String getUserReadableName() {
        String fingerprint = TextUtils.isEmpty(Build.FINGERPRINT) ? "" : Build.FINGERPRINT;
        String manufacturer = TextUtils.isEmpty(Build.MANUFACTURER) ? "" : Build.MANUFACTURER;
        String product = TextUtils.isEmpty(Build.PRODUCT) ? "" : Build.PRODUCT
                .replace("aokp_", "")
                .replace("aosp_", "")
                .replace("cm_", "")
                .replace("lineage_", "");
        String model = TextUtils.isEmpty(Build.MODEL) ? "" : Build.MODEL;
        String device = TextUtils.isEmpty(Build.DEVICE) ? "" : Build.DEVICE;
        String result = (fingerprint.toLowerCase().contains(product.toLowerCase())
                || product.toLowerCase().contains(device.toLowerCase())
                || device.toLowerCase().contains(product.toLowerCase())) ? model : product;
        if (!result.toLowerCase().contains(manufacturer.toLowerCase())) {
            result = manufacturer + " " + result;
        }
        if (TextUtils.isEmpty(result)) {
            return "";
        }
        return (result.substring(0, 1).toUpperCase() + result.substring(1))
                .replace("\n", " ")
                .replace("\r", " ")
                .replace(",", " ")
                .trim()
                ;
    }

    private Map<String, String> getBuildValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Build.HARDWARE", Build.HARDWARE);
        values.put("Build.RADIO", Build.RADIO);
        values.put("Build.BOOTLOADER", Build.BOOTLOADER);
        values.put("Build.FINGERPRINT", Build.FINGERPRINT);
        values.put("Build.BRAND", Build.BRAND);
        values.put("Build.DEVICE", Build.DEVICE);
        values.put("Build.VERSION.SDK_INT", Integer.toString(Build.VERSION.SDK_INT));
        values.put("Build.MODEL", Build.MODEL);
        values.put("Build.MANUFACTURER", Build.MANUFACTURER);
        values.put("Build.PRODUCT", Build.PRODUCT);
        values.put("Build.ID", Build.ID);
        values.put("Build.VERSION.RELEASE", Build.VERSION.RELEASE);
        return values;
    }

    private Map<String, String> getConfigurationValues() {
        Map<String, String> values = new LinkedHashMap<>();
        Configuration config = context.getResources().getConfiguration();
        values.put("TouchScreen", Integer.toString(config.touchscreen));
        values.put("Keyboard", Integer.toString(config.keyboard));
        values.put("Navigation", Integer.toString(config.navigation));
        values.put("ScreenLayout", Integer.toString(config.screenLayout & 15));
        values.put("HasHardKeyboard", Boolean.toString(config.keyboard == Configuration.KEYBOARD_QWERTY));
        values.put("HasFiveWayNavigation", Boolean.toString(config.navigation == Configuration.NAVIGATIONHIDDEN_YES));
        values.put("GL.Version", Integer.toString(((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getDeviceConfigurationInfo().reqGlEsVersion));
        return values;
    }

    private Map<String, String> getDisplayMetricsValues() {
        Map<String, String> values = new LinkedHashMap<>();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        values.put("Screen.Density", Integer.toString(metrics.densityDpi));
        values.put("Screen.Width", Integer.toString(metrics.widthPixels));
        values.put("Screen.Height", Integer.toString(metrics.heightPixels));
        return values;
    }

    private Map<String, String> getPackageManagerValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Platforms", TextUtils.join(",", NativeDeviceInfoProvider.getPlatforms()));
        values.put("SharedLibraries", TextUtils.join(",", NativeDeviceInfoProvider.getSharedLibraries(context)));
        values.put("Features", TextUtils.join(",", NativeDeviceInfoProvider.getFeatures(context)));
        values.put("Locales", TextUtils.join(",", NativeDeviceInfoProvider.getLocales(context)));
        NativeGsfVersionProvider gsfVersionProvider = new NativeGsfVersionProvider(context);
        values.put("GSF.version", Integer.toString(gsfVersionProvider.getGsfVersionCode(false)));
        values.put("Vending.version", Integer.toString(gsfVersionProvider.getVendingVersionCode(false)));
        values.put("Vending.versionString", gsfVersionProvider.getVendingVersionString(false));
        return values;
    }

    private Map<String, String> getOperatorValues() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("CellOperator", tm.getNetworkOperator());
        values.put("SimOperator", tm.getSimOperator());
        return values;
    }
}
