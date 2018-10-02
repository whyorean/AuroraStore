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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;

import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.percolate.caffeine.PhoneUtils;
import com.percolate.caffeine.ViewUtils;

import org.json.JSONArray;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.dragons.aurora.Aurora.PRIVILEGED_EXTENSION_PACKAGE_NAME;
import static com.dragons.aurora.Aurora.PRIVILEGED_EXTENSION_SERVICE_INTENT;

public class Util {

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
                && (app.isInPlayStore() || app.getPackageName().equals(BuildConfig.APPLICATION_ID) && !isAlreadyQueued(app));
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
        if (context != null) {
            String Theme = Prefs.getString(context, "PREFERENCE_THEME");
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
        return false;
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

    public static void reloadRecycler(RecyclerView recyclerView) {
        Context context = recyclerView.getContext();
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(context, R.anim.anim_falldown);
        recyclerView.setLayoutAnimation(controller);
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    public static void setColors(Context mContext, SwipeRefreshLayout swipeRefreshLayout) {
        swipeRefreshLayout.setColorSchemeColors(
                mContext.getResources().getColor(R.color.colorAccent),
                mContext.getResources().getColor(R.color.colorGreen),
                mContext.getResources().getColor(R.color.colorRed),
                mContext.getResources().getColor(R.color.colorOrange),
                mContext.getResources().getColor(R.color.colorGold)
        );
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

    public static boolean isConnected(Context c) {
        return PhoneUtils.isNetworkAvailable(c);
    }

    public static void hide(View v, int viewID) {
        ViewUtils.findViewById(v, viewID).setVisibility(View.GONE);
    }

    public static void show(View v, int viewID) {
        ViewUtils.findViewById(v, viewID).setVisibility(View.VISIBLE);
    }

    public static void setText(View v, int viewId, String text) {
        TextView textView = ViewUtils.findViewById(v, viewId);
        if (null != textView)
            textView.setText(text);
    }

    public static void setText(View v, int viewId, int stringId, Object... text) {
        setText(v, viewId, v.getResources().getString(stringId, text));
    }

    public static GooglePlayAPI.SUBCATEGORY getSubCategory(Context context) {
        GooglePlayAPI.SUBCATEGORY subcategory = null;
        switch (Prefs.getString(context, "PREFERENCE_SUBCATEGORY")) {
            case "1":
                subcategory = GooglePlayAPI.SUBCATEGORY.TOP_FREE;
                break;
            case "2":
                subcategory = GooglePlayAPI.SUBCATEGORY.TOP_GROSSING;
                break;
            case "3":
                subcategory = GooglePlayAPI.SUBCATEGORY.MOVERS_SHAKERS;
                break;
            default:
                subcategory = GooglePlayAPI.SUBCATEGORY.TOP_GROSSING;
        }
        return subcategory;
    }

    public static int getThemeFromPref(Context context) {
        String Theme = Prefs.getString(context, "PREFERENCE_THEME");
        switch (Theme) {
            case "Light":
                return R.style.AppTheme;
            case "Dark":
                return R.style.AppTheme_Dark;
            case "Black":
                return R.style.AppTheme_Black;
            default:
                return R.style.AppTheme;
        }
    }

    public static void setText(TextView textView, String text) {
        if (!TextUtils.isEmpty(text)) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    public static String[] getStringArray(JSONArray array) {
        if (array == null)
            return null;
        String[] arr = new String[array.length()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = array.optString(i);
        }
        return arr;
    }

    public static boolean isExtensionInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(PRIVILEGED_EXTENSION_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isExtensionAvailable(Context context) {
        if (!isExtensionInstalled(context)) {
            return false;
        }
        ServiceConnection serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };
        Intent serviceIntent = new Intent(PRIVILEGED_EXTENSION_SERVICE_INTENT);
        serviceIntent.setPackage(PRIVILEGED_EXTENSION_PACKAGE_NAME);

        try {
            context.getApplicationContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
