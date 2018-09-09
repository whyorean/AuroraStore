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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.dragons.aurora.helpers.Prefs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import timber.log.Timber;

public class SpoofDeviceManager {

    static private final String DEVICES_LIST_KEY = "DEVICE_LIST_" + BuildConfig.VERSION_NAME;
    static private final String SPOOF_FILE_PREFIX = "device-";
    static private final String SPOOF_FILE_SUFFIX = ".properties";

    private Context context;

    public SpoofDeviceManager(Context context) {
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

    public Properties getProperties(String entryName) {
        File defaultDirectoryFile = new File(Paths.getDownloadPath(context), entryName);
        if (defaultDirectoryFile.exists()) {
            Timber.i("Loading device info from %s", defaultDirectoryFile.getAbsolutePath());
            return getProperties(defaultDirectoryFile);
        } else {
            Timber.i("Loading device info from " + getApkFile() + "/" + entryName);
            JarFile jarFile = getApkAsJar();
            if (null == jarFile || null == jarFile.getEntry(entryName)) {
                Properties empty = new Properties();
                empty.setProperty("Could not read ", entryName);
                return empty;
            }
            return getProperties(jarFile, (JarEntry) jarFile.getEntry(entryName));
        }
    }

    private Properties getProperties(JarFile jarFile, JarEntry entry) {
        Properties properties = new Properties();
        try {
            properties.load(jarFile.getInputStream(entry));
        } catch (IOException e) {
            Timber.e("Could not read %s", entry.getName());
        }
        return properties;
    }

    private Properties getProperties(File file) {
        Properties properties = new Properties();
        try {
            properties.load(new BufferedInputStream(new FileInputStream(file)));
        } catch (IOException e) {
            Timber.e("Could not read %s", file.getName());
        }
        return properties;
    }

    private Map<String, String> getDevicesFromSharedPreferences() {
        Set<String> deviceNames = Prefs.getStringSet(context, DEVICES_LIST_KEY);
        Map<String, String> devices = new HashMap<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (String name : deviceNames) {
            devices.put(name, prefs.getString(name, ""));
        }
        return devices;
    }

    private void putDevicesToSharedPreferences(Map<String, String> devices) {
        Prefs.putStringSet(context, DEVICES_LIST_KEY, devices.keySet());
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        for (String name : devices.keySet()) {
            prefs.putString(name, devices.get(name));
        }
        prefs.apply();
    }

    private Map<String, String> getDevicesFromApk() {
        JarFile jarFile = getApkAsJar();
        Map<String, String> deviceNames = new HashMap<>();
        if (null == jarFile) {
            return deviceNames;
        }
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!filenameValid(entry.getName())) {
                continue;
            }
            deviceNames.put(entry.getName(), getProperties(jarFile, entry).getProperty("UserReadableName"));
        }
        return deviceNames;
    }

    private JarFile getApkAsJar() {
        File apk = getApkFile();
        try {
            if (null != apk && apk.exists()) {
                return new JarFile(apk);
            }
        } catch (IOException e) {
            Timber.e("Could not open Aurora Store apk as a jar file: %s", e.getMessage());
        }
        return null;
    }

    private File getApkFile() {
        try {
            String sourceDir = context.getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0).sourceDir;
            if (!TextUtils.isEmpty(sourceDir)) {
                return new File(sourceDir);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Having a currently running app uninstalled is unlikely
        }
        return null;
    }

    private Map<String, String> getDevicesFromDownloadDirectory() {
        Map<String, String> deviceNames = new HashMap<>();
        File defaultDir = Paths.getDownloadPath(context);
        if (!defaultDir.exists() || null == defaultDir.listFiles()) {
            return deviceNames;
        }
        for (File file : defaultDir.listFiles()) {
            if (!file.isFile() || !filenameValid(file.getName())) {
                continue;
            }
            deviceNames.put(file.getName(), getProperties(file).getProperty("UserReadableName"));
        }
        return deviceNames;
    }
}
