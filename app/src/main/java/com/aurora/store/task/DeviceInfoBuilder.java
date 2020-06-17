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
import android.content.res.Configuration;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.aurora.store.provider.EglExtensionProvider;
import com.aurora.store.provider.NativeDeviceInfoProvider;
import com.aurora.store.provider.NativeGsfVersionProvider;
import com.aurora.store.util.Log;
import com.aurora.store.util.PathUtil;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

public class DeviceInfoBuilder {

    static private Map<String, String> staticProperties = new HashMap<>();

    static {
        staticProperties.put("Client", "android-google");
        staticProperties.put("Roaming", "mobile-notroaming");
        staticProperties.put("TimeZone", TimeZone.getDefault().getID());
        staticProperties.put("GL.Extensions", TextUtils.join(",", EglExtensionProvider.getEglExtensions()));
    }

    private Context context;

    public DeviceInfoBuilder(Context context) {
        this.context = context;
    }

    private static String buildProperties(Map<String, String> properties) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (String key : properties.keySet()) {
            stringBuilder.append(key).append(" = ").append(properties.get(key)).append("\n");
        }
        return stringBuilder.toString();
    }

    public boolean build() {
        final File file = new File(PathUtil.getExtBaseDirectory(context), "device-" + Build.DEVICE + ".properties");
        final String content = buildProperties(getDeviceInfo());

        try {
            if (!file.exists())
                PathUtil.createBaseDirectory(context);

            final Writer writer = new FileWriter(file);
            writer.write(content);
            writer.flush();
            return true;
        } catch (IOException e) {
            Log.e("Failed to write device info");
            return false;
        }
    }

    private Map<String, String> getDeviceInfo() {
        final Map<String, String> values = new LinkedHashMap<>();
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
        String product = Build.PRODUCT;
        String model = Build.MODEL;
        String device = Build.DEVICE;

        if (!product.isEmpty())
            return product;
        else if (!model.isEmpty())
            return model;
        else if (!device.isEmpty())
            return device;
        else
            return "Unknown";
    }

    private Map<String, String> getBuildValues() {
        final Map<String, String> values = new LinkedHashMap<>();
        values.put("Build.HARDWARE", Build.HARDWARE);
        values.put("Build.RADIO", StringUtils.isNotEmpty(Build.getRadioVersion()) ? Build.getRadioVersion() : StringUtils.EMPTY);
        values.put("Build.BOOTLOADER", Build.BOOTLOADER);
        values.put("Build.FINGERPRINT", Build.FINGERPRINT);
        values.put("Build.BRAND", Build.BRAND);
        values.put("Build.DEVICE", Build.DEVICE);
        values.put("Build.VERSION.SDK_INT", String.valueOf(Build.VERSION.SDK_INT));
        values.put("Build.MODEL", Build.MODEL);
        values.put("Build.MANUFACTURER", Build.MANUFACTURER);
        values.put("Build.PRODUCT", Build.PRODUCT);
        values.put("Build.ID", Build.ID);
        values.put("Build.VERSION.RELEASE", Build.VERSION.RELEASE);
        return values;
    }

    private Map<String, String> getConfigurationValues() {
        final Map<String, String> values = new LinkedHashMap<>();
        final Configuration configuration = context.getResources().getConfiguration();
        values.put("TouchScreen", String.valueOf(configuration.touchscreen));
        values.put("Keyboard", String.valueOf(configuration.keyboard));
        values.put("Navigation", String.valueOf(configuration.navigation));
        values.put("ScreenLayout", String.valueOf(configuration.screenLayout & 15));
        values.put("HasHardKeyboard", String.valueOf(configuration.keyboard == Configuration.KEYBOARD_QWERTY));
        values.put("HasFiveWayNavigation", String.valueOf(configuration.navigation == Configuration.NAVIGATIONHIDDEN_YES));

        final Object object = context.getSystemService(Context.ACTIVITY_SERVICE);
        final ActivityManager activityManager = (ActivityManager) object;

        if (activityManager != null) {
            values.put("GL.Version", String.valueOf(activityManager.getDeviceConfigurationInfo().reqGlEsVersion));
        }
        return values;
    }

    private Map<String, String> getDisplayMetricsValues() {
        final Map<String, String> values = new LinkedHashMap<>();
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        values.put("Screen.Density", String.valueOf(metrics.densityDpi));
        values.put("Screen.Width", String.valueOf(metrics.widthPixels));
        values.put("Screen.Height", String.valueOf(metrics.heightPixels));
        return values;
    }

    private Map<String, String> getPackageManagerValues() {
        final Map<String, String> values = new LinkedHashMap<>();
        values.put("SharedLibraries", TextUtils.join(",", NativeDeviceInfoProvider.getSharedLibraries(context)));
        values.put("Features", TextUtils.join(",", NativeDeviceInfoProvider.getFeatures(context)));
        values.put("Locales", TextUtils.join(",", NativeDeviceInfoProvider.getLocales(context)));

        final NativeGsfVersionProvider gsfVersionProvider = new NativeGsfVersionProvider(context);
        values.put("GSF.version", String.valueOf(gsfVersionProvider.getGsfVersionCode(false)));
        values.put("Vending.version", String.valueOf(gsfVersionProvider.getVendingVersionCode(false)));
        values.put("Vending.versionString", gsfVersionProvider.getVendingVersionString(false));
        return values;
    }

    private Map<String, String> getOperatorValues() {
        final Object object = context.getSystemService(Context.TELEPHONY_SERVICE);
        final TelephonyManager telephonyManager = (TelephonyManager) object;
        final Map<String, String> values = new LinkedHashMap<>();
        if (telephonyManager != null) {
            values.put("CellOperator", telephonyManager.getNetworkOperator());
            values.put("SimOperator", telephonyManager.getSimOperator());
        }
        return values;
    }
}
