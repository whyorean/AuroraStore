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

package com.aurora.store.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.aurora.store.BuildConfig;
import com.aurora.store.util.Log;
import com.aurora.store.util.PathUtil;
import com.aurora.store.util.PrefUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class SpoofManager {

    static private final String DEVICES_LIST_KEY = "DEVICE_LIST_" + BuildConfig.VERSION_NAME;
    static private final String SPOOF_FILE_PREFIX = "device-";
    static private final String SPOOF_FILE_SUFFIX = ".properties";

    private Context context;

    public SpoofManager(Context context) {
        this.context = context;
    }

    static private boolean filenameValid(String filename) {
        return filename.startsWith(SPOOF_FILE_PREFIX) && filename.endsWith(SPOOF_FILE_SUFFIX);
    }

    public Map<String, String> getDevices() {
        Map<String, String> devices = getDevicesFromSharedPreferences();
        if (devices.isEmpty()) {
            devices = getDevicesFromApk();
            putDevicesToSharedPreferences(devices);
        }
        devices.putAll(getDevicesFromDownloadDirectory());
        return devices;
    }

    public List<Properties> getAvailableDevice() {
        final List<Properties> propertiesList = new ArrayList<>();
        final Properties defaultProperties = new Properties();
        defaultProperties.setProperty("Build.MODEL", "Default");
        defaultProperties.setProperty("Build.MANUFACTURER", Build.MANUFACTURER);
        defaultProperties.setProperty("Build.VERSION.SDK_INT", String.valueOf(Build.VERSION.SDK_INT));
        propertiesList.add(0, defaultProperties);
        propertiesList.addAll(getSpoofDevicesFromApk());
        propertiesList.addAll(getSpoofDevicesFromUser());
        return propertiesList;
    }

    private List<Properties> getSpoofDevicesFromApk() {
        final JarFile jarFile = getApkAsJar();
        final List<Properties> propertiesList = new ArrayList<>();

        if (null == jarFile) {
            return propertiesList;
        }

        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (!filenameValid(entry.getName())) {
                continue;
            }
            propertiesList.add(getProperties(jarFile, entry));
        }
        return propertiesList;
    }

    private List<Properties> getSpoofDevicesFromUser() {
        final List<Properties> deviceNames = new ArrayList<>();
        final File defaultDir = new File(PathUtil.getRootApkPath(context));
        final File[] files = defaultDir.listFiles();
        if (defaultDir.exists() && files != null) {
            for (File file : files) {
                if (!file.isFile() || !filenameValid(file.getName())) {
                    continue;
                }
                deviceNames.add(getProperties(file));
            }
        }
        return deviceNames;
    }

    public Properties getProperties(String entryName) {
        final File defaultDirectoryFile = new File(PathUtil.getRootApkPath(context), entryName);
        if (defaultDirectoryFile.exists()) {
            Log.i("Loading device info from %s", defaultDirectoryFile.getAbsolutePath());
            return getProperties(defaultDirectoryFile);
        } else {
            Log.i("Loading device info from " + getApkFile() + "/" + entryName);
            final JarFile jarFile = getApkAsJar();
            if (null == jarFile || null == jarFile.getEntry(entryName)) {
                final Properties empty = new Properties();
                empty.setProperty("Could not read ", entryName);
                return empty;
            }
            return getProperties(jarFile, (JarEntry) jarFile.getEntry(entryName));
        }
    }

    private Properties getProperties(JarFile jarFile, JarEntry entry) {
        final Properties properties = new Properties();
        try {
            properties.load(jarFile.getInputStream(entry));
            properties.setProperty("CONFIG_NAME", entry.getName());
        } catch (IOException e) {
            Log.e("Could not read %s", entry.getName());
        }
        return properties;
    }

    private Properties getProperties(File file) {
        final Properties properties = new Properties();
        try {
            properties.load(new BufferedInputStream(new FileInputStream(file)));
            properties.setProperty("CONFIG_NAME", file.getName());
        } catch (IOException e) {
            Log.e("Could not read %s", file.getName());
        }
        return properties;
    }

    private Map<String, String> getDevicesFromSharedPreferences() {
        Set<String> deviceNames = PrefUtil.getStringSet(context, DEVICES_LIST_KEY);
        Map<String, String> devices = new HashMap<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (String name : deviceNames) {
            devices.put(name, prefs.getString(name, ""));
        }
        return devices;
    }

    private void putDevicesToSharedPreferences(Map<String, String> devices) {
        PrefUtil.putStringSet(context, DEVICES_LIST_KEY, devices.keySet());
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        for (String name : devices.keySet()) {
            prefs.putString(name, devices.get(name));
        }
        prefs.apply();
    }

    private Map<String, String> getDevicesFromApk() {
        final JarFile jarFile = getApkAsJar();
        final Map<String, String> deviceNames = new HashMap<>();
        if (null == jarFile) {
            return deviceNames;
        }
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (!filenameValid(entry.getName())) {
                continue;
            }
            deviceNames.put(entry.getName(), getProperties(jarFile, entry).getProperty("UserReadableName"));
        }
        return deviceNames;
    }

    private JarFile getApkAsJar() {
        final File file = getApkFile();
        try {
            if (null != file && file.exists()) {
                return new JarFile(file);
            }
        } catch (IOException e) {
            Log.e("Could not open Aurora Store apk as a jar file: %s", e.getMessage());
        }
        return null;
    }

    private File getApkFile() {
        try {
            final String sourceDir = context.getPackageManager()
                    .getApplicationInfo(BuildConfig.APPLICATION_ID, 0).sourceDir;
            if (!TextUtils.isEmpty(sourceDir)) {
                return new File(sourceDir);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Having a currently running app uninstalled is unlikely
        }
        return null;
    }

    private Map<String, String> getDevicesFromDownloadDirectory() {
        final Map<String, String> deviceNames = new HashMap<>();
        final File defaultDir = new File(PathUtil.getRootApkPath(context));
        if (!defaultDir.exists() || null == defaultDir.listFiles()) {
            return deviceNames;
        }
        for (File file : defaultDir.listFiles()) {
            if (!file.isFile() || !filenameValid(file.getName())) {
                continue;
            }
            String name = getProperties(file).getProperty("UserReadableName");
            if (name != null) {
                deviceNames.put(file.getName(), name);
            }
        }
        return deviceNames;
    }
}
