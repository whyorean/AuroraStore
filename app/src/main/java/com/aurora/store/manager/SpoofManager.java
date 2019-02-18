package com.aurora.store.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.aurora.store.BuildConfig;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PathUtil;
import com.aurora.store.utility.PrefUtil;

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

    public Properties getProperties(String entryName) {
        File defaultDirectoryFile = new File(PathUtil.getRootApkPath(context), entryName);
        if (defaultDirectoryFile.exists()) {
            Log.i("Loading device info from %s", defaultDirectoryFile.getAbsolutePath());
            return getProperties(defaultDirectoryFile);
        } else {
            Log.i("Loading device info from " + getApkFile() + "/" + entryName);
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
            Log.e("Could not read %s", entry.getName());
        }
        return properties;
    }

    private Properties getProperties(File file) {
        Properties properties = new Properties();
        try {
            properties.load(new BufferedInputStream(new FileInputStream(file)));
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
            Log.e("Could not open Aurora Store apk as a jar file: %s", e.getMessage());
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
        File defaultDir = new File(PathUtil.getRootApkPath(context));
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
