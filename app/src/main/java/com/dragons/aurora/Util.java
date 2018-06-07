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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.model.App;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Util {

    private static final String DELIMITER = ",";
    private static final Map<Integer, String> siPrefixes = new HashMap<>();
    private static final Map<Integer, String> diPrefixes = new HashMap<>();

    static {
        siPrefixes.put(0, "");
        siPrefixes.put(3, " KB");
        siPrefixes.put(6, " MB");
        siPrefixes.put(9, " GB");
    }

    static {
        diPrefixes.put(0, "");
        diPrefixes.put(3, " K");
        diPrefixes.put(6, " Million");
        diPrefixes.put(9, " Billion");
    }

    static public Map<String, String> sort(Map<String, String> unsorted) {

        class CaseInsensitiveComparator implements Comparator<String> {

            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        }

        Map<String, String> sortedByKey = new TreeMap<>(new CaseInsensitiveComparator());
        sortedByKey.putAll(swapKeysValues(unsorted));
        Map<String, String> sorted = new LinkedHashMap<>();
        for (String value : sortedByKey.keySet()) {
            sorted.put(sortedByKey.get(value), value);
        }
        return sorted;
    }

    private static <K, V> Map<V, K> swapKeysValues(Map<K, V> map) {
        Map<V, K> swapped = new HashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            swapped.put(entry.getValue(), entry.getKey());
        }
        return swapped;
    }

    static public Map<String, String> addToStart(LinkedHashMap<String, String> map, String key, String value) {
        LinkedHashMap<String, String> clonedMap = (LinkedHashMap<String, String>) map.clone();
        map.clear();
        map.put(key, value);
        map.putAll(clonedMap);
        return map;
    }

    static public Set<String> getStringSet(Context context, String key) {
        return new HashSet<>(Arrays.asList(TextUtils.split(
                PreferenceManager.getDefaultSharedPreferences(context).getString(key, ""),
                DELIMITER
        )));
    }

    static public void putStringSet(Context context, String key, Set<String> set) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, TextUtils.join(DELIMITER, set)).apply();
    }

    static public void putBoolean(Context context, String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
    }


    public static Boolean getBoolean(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false);
    }

    static public int parseInt(String intAsString, int defaultValue) {
        try {
            return Integer.parseInt(intAsString);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static public void closeSilently(Closeable closeable) {
        if (null == closeable) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            // Closing silently
        }
    }

    static public String addSiPrefix(Integer integer) {
        int tempValue = integer;
        int order = 0;
        while (tempValue >= 1000.0) {
            tempValue /= 1000.0;
            order += 3;
        }
        return tempValue + siPrefixes.get(order);
    }

    static public String addDiPrefix(Integer integer) {
        int tempValue = integer;
        int order = 0;
        while (tempValue >= 1000.0) {
            tempValue /= 1000.0;
            order += 3;
        }
        return tempValue + diPrefixes.get(order);
    }

    public static boolean isAlreadyDownloaded(Context context, App app) {
        return Paths.getApkPath(context, app.getPackageName(), app.getVersionCode()).exists()
                && DownloadState.get(app.getPackageName()).isEverythingSuccessful();
    }

    public static boolean shouldDownload(Context context, App app) {
        File apk = Paths.getApkPath(context, app.getPackageName(), app.getVersionCode());
        return (!apk.exists() || apk.length() != app.getSize() || !DownloadState.get(app.getPackageName()).isEverythingSuccessful())
                && (app.isInPlayStore() || app.getPackageName().equals(BuildConfig.APPLICATION_ID));
    }

    public static boolean isAlreadyQueued(App app) {
        DownloadState state = DownloadState.get(app.getPackageName());
        if (state != null && !state.isEverythingFinished())
            return true;
        else
            return false;
    }

    public static boolean isAlreadyInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static Intent getLaunchIntent(Activity activity, App app) {
        Intent i = activity.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
        boolean isTv = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && ((AuroraApplication) activity.getApplication()).isTv();
        if (isTv) {
            Intent l = activity.getPackageManager().getLeanbackLaunchIntentForPackage(app.getPackageName());
            if (null != l) {
                i = l;
            }
        }
        if (i == null) {
            return null;
        }
        i.addCategory(isTv ? Intent.CATEGORY_LEANBACK_LAUNCHER : Intent.CATEGORY_LAUNCHER);
        return i;
    }

    public static boolean isDark(Context context) {
        String Theme = PreferenceFragment.getString(context, "PREFERENCE_THEME");
        switch (Theme) {
            case "Light":
                return false;
            case "Dark":
                return true;
            case "Black":
                return true;
            default:
                return false;
        }
    }

    public static int getStyledAttribute(Context context, int styleID) {
        TypedArray arr = context.obtainStyledAttributes(new TypedValue().data, new int[]{styleID});
        int styledColor = arr.getColor(0, -1);
        arr.recycle();
        return styledColor;
    }

    public static GradientDrawable getGradient(int startColor, int endColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        gd.setColors(new int[]{startColor, endColor});
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gd.setDither(true);
        gd.setSize(200, 200);
        return gd;
    }

    public static void reloadRecycler(final RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();
        final LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_anim);
        recyclerView.setLayoutAnimation(controller);
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    public static String getSimpleName(String oldName) {
        if (oldName.contains(":"))
            return oldName.substring(0, oldName.indexOf(":"));
        else if (oldName.contains("&"))
            return oldName.substring(0, oldName.indexOf("&"));
        else if (oldName.contains("-"))
            return oldName.substring(0, oldName.indexOf("-"));
        else if (oldName.contains("+"))
            return oldName.substring(0, oldName.indexOf("+"));
        else if (oldName.contains("("))
            return oldName.substring(0, oldName.indexOf("("));
        else return oldName;
    }
}
