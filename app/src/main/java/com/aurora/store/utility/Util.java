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

package com.aurora.store.utility;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.receiver.UpdatesReceiver;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2core.Downloader;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLHandshakeException;

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

    static public void addToStart(LinkedHashMap<String, String> map, String key, String value) {
        LinkedHashMap<String, String> clonedMap = (LinkedHashMap<String, String>) map.clone();
        map.clear();
        map.put(key, value);
        map.putAll(clonedMap);
    }

    static public String addSiPrefix(Long value) {
        long tempValue = value;
        int order = 0;
        while (tempValue >= 1000.0) {
            tempValue /= 1000.0;
            order += 3;
        }
        return tempValue + siPrefixes.get(order);
    }

    static public String addDiPrefix(Long value) {
        long tempValue = value;
        int order = 0;
        while (tempValue >= 1000.0) {
            tempValue /= 1000.0;
            order += 3;
        }
        return tempValue + diPrefixes.get(order);
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(
                Constants.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    public static long parseLong(String intAsString, long defaultValue) {
        try {
            return Long.parseLong(intAsString);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int parseInt(String intAsString, int defaultValue) {
        try {
            return Integer.parseInt(intAsString);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean noNetwork(Throwable e) {
        return e instanceof UnknownHostException
                || e instanceof SSLHandshakeException
                || e instanceof SocketException
                || e instanceof SocketTimeoutException
                || (null != e && null != e.getCause() && noNetwork(e.getCause()));
    }

    public static int getColorAttribute(Context context, int styleID) {
        TypedArray arr = context.obtainStyledAttributes(new TypedValue().data, new int[]{styleID});
        int styledColor = arr.getColor(0, -1);
        arr.recycle();
        return styledColor;
    }

    @NonNull
    public static String getETAString(@NonNull final Context context, final long etaInMilliSeconds) {
        if (etaInMilliSeconds < 0) {
            return "";
        }
        int seconds = (int) (etaInMilliSeconds / 1000);
        long hours = seconds / 3600;
        seconds -= hours * 3600;
        long minutes = seconds / 60;
        seconds -= minutes * 60;
        if (hours > 0) {
            return context.getString(R.string.download_eta_hrs, hours, minutes, seconds);
        } else if (minutes > 0) {
            return context.getString(R.string.download_eta_min, minutes, seconds);
        } else {
            return context.getString(R.string.download_eta_sec, seconds);
        }
    }

    @NonNull
    public static String getDownloadSpeedString(@NonNull Context context, long downloadedBytesPerSecond) {
        if (downloadedBytesPerSecond < 0) {
            return "";
        }
        double kb = (double) downloadedBytesPerSecond / (double) 1000;
        double mb = kb / (double) 1000;
        final DecimalFormat decimalFormat = new DecimalFormat(".##");
        if (mb >= 1) {
            return context.getString(R.string.download_speed_mb, decimalFormat.format(mb));
        } else if (kb >= 1) {
            return context.getString(R.string.download_speed_kb, decimalFormat.format(kb));
        } else {
            return context.getString(R.string.download_speed_bytes, downloadedBytesPerSecond);
        }
    }

    public static String humanReadableByteSpeed(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(Locale.getDefault(), "%.1f %sB/s",
                bytes / Math.pow(unit, exp), pre);
    }

    public static String humanReadableByteValue(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(Locale.getDefault(), "%.1f %sB",
                bytes / Math.pow(unit, exp), pre);
    }

    public static String getStatus(Status status) {
        switch (status) {
            case NONE:
                return "None";
            case ADDED:
                return "Added";
            case FAILED:
                return "Failed";
            case PAUSED:
                return "Paused";
            case QUEUED:
                return "Queued";
            case DELETED:
                return "Deleted";
            case REMOVED:
                return "Removed";
            case CANCELLED:
                return "Cancelled";
            case COMPLETED:
                return "Completed";
            case DOWNLOADING:
                return "Downloading";
            default:
                return "--";
        }
    }

    public static String getTheme(Context context) {
        return getPrefs(context).getString(Constants.PREFERENCE_THEME, "light");
    }

    public static boolean isLegacyCardEnabled(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_UI_CARD_STYLE, true);
    }

    public static boolean snapPagerEnabled(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_FEATURED_SNAP, false);
    }

    public static boolean isIMEEnabled(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_SEARCH_IME, true);
    }

    public static boolean isTransparentStyle(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_UI_TRANSPARENT, true);
    }

    public static boolean isExtendedUpdatesEnabled(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_UPDATES_EXTENDED, false);
    }

    public static boolean filterGoogleAppsEnabled(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_FILTER_GOOGLE, false);
    }

    public static boolean filterFDroidAppsEnabled(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_FILTER_F_DROID, true);
    }

    public static boolean filterSearchNonPersistent(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_FILTER_SEARCH, true);
    }

    public static boolean isDownloadWifiOnly(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_DOWNLOAD_WIFI, false);
    }

    public static boolean isPrivilegedInstall(Context context) {
        String prefValue = PrefUtil.getString(context, Constants.PREFERENCE_INSTALLATION_METHOD);
        switch (prefValue) {
            case "0":
                return false;
            case "1":
            case "2":
                return true;
            default:
                return false;
        }
    }

    public static boolean shouldDeleteApk(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && Util.isRootInstallEnabled(context)) {
            return true;
        } else
            return getPrefs(context).getBoolean(Constants.PREFERENCE_INSTALLATION_DELETE, false);
    }

    public static boolean isNativeInstallerEnforced(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_INSTALLATION_TYPE, false);
    }

    public static boolean shouldAutoInstallApk(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_INSTALLATION_AUTO, true);
    }

    public static int getActiveDownloadCount(Context context) {
        return getPrefs(context).getInt(Constants.PREFERENCE_DOWNLOAD_ACTIVE, 3);
    }

    public static boolean isFetchDebugEnabled(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_DOWNLOAD_DEBUG, false);
    }

    public static boolean isNetworkProxyEnabled(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_ENABLE_PROXY, false);
    }

    public static boolean isCustomLocaleEnabled(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_LOCALE_CUSTOM, false);
    }

    public static boolean isTabScrollable(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_TAB_MODE, false);
    }

    public static boolean isRootInstallEnabled(Context context) {
        String installMethod = getPrefs(context).getString(Constants.PREFERENCE_INSTALLATION_METHOD, "0");
        return installMethod.equals("1");
    }

    public static boolean isCustomTokenizerEnabled(Context context) {
        return getPrefs(context).getBoolean(Constants.PREFERENCE_ENABLE_CUSTOM_TOKENIZER, false);
    }

    public static String getCustomTokenizerURL(Context context) {
        return getPrefs(context).getString(Constants.PREFERENCE_CUSTOM_TOKENIZER, "");
    }

    public static Proxy.Type getProxyType(Context context) {
        String proxyType = getPrefs(context).getString(Constants.PREFERENCE_PROXY_TYPE, "HTTP");
        switch (proxyType) {
            case "HTTP":
                return Proxy.Type.HTTP;
            case "SOCKS":
                return Proxy.Type.SOCKS;
            case "DIRECT":
                return Proxy.Type.DIRECT;
            default:
                return Proxy.Type.HTTP;
        }
    }

    public static DiskCacheStrategy getCacheStrategy(Context context) {
        String proxyType = getPrefs(context).getString(Constants.PREFERENCE_CACHE_STRATEGY, "0");
        switch (proxyType) {
            case "0":
                return DiskCacheStrategy.AUTOMATIC;
            case "1":
                return DiskCacheStrategy.ALL;
            case "2":
                return DiskCacheStrategy.NONE;
            default:
                return DiskCacheStrategy.AUTOMATIC;
        }
    }

    public static String getInstallationProfile(Context context) {
        if (!Util.isRootInstallEnabled(context))
            return "0";
        else
            return getPrefs(context).getString(Constants.PREFERENCE_INSTALLATION_PROFILE, "0");
    }

    public static Proxy getNetworkProxy(Context context) {
        String proxyHost = getPrefs(context).getString(Constants.PREFERENCE_PROXY_HOST, "127.0.0.1");
        String proxyPort = getPrefs(context).getString(Constants.PREFERENCE_PROXY_PORT, "8118");
        int port = Util.parseInt(proxyPort, 8118);
        return new Proxy(getProxyType(context), new InetSocketAddress(proxyHost, port));
    }

    public static Downloader.FileDownloaderType getDownloadStrategy(Context context) {
        String prefValue = getPrefs(context).getString(Constants.PREFERENCE_DOWNLOAD_STRATEGY, "");
        switch (prefValue) {
            case "0":
                return Downloader.FileDownloaderType.SEQUENTIAL;
            case "1":
                return Downloader.FileDownloaderType.PARALLEL;
            default:
                return Downloader.FileDownloaderType.PARALLEL;
        }
    }

    public static int getDefaultTab(Context context) {
        String value = getPrefs(context).getString(Constants.PREFERENCE_DEFAULT_TAB, "0");
        return parseInt(value, 0);
    }

    public static boolean isCacheObsolete(Context context) {
        try {
            long lastSyncDate = Long.parseLong(PrefUtil.getString(context, Constants.PREFERENCE_CACHE_DATE));
            long currentSyncDate = Calendar.getInstance().getTimeInMillis();
            long diffDatesInMillis = currentSyncDate - lastSyncDate;
            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffDatesInMillis);
            return diffInDays > 3;
        } catch (Exception e) {
            return false;
        }
    }

    public static void setCacheCreateTime(Context context, Long dateInMillis) {
        PrefUtil.putString(context, Constants.PREFERENCE_CACHE_DATE, String.valueOf(dateInMillis));
    }

    public static void clearCache(Context context) {
        PrefUtil.putString(context, Constants.PREFERENCE_TOP_APPS, "");
        PrefUtil.putString(context, Constants.PREFERENCE_TOP_GAMES, "");
        PrefUtil.putString(context, Constants.PREFERENCE_TOP_FAMILY, "");
    }

    public static void restartApp(Context context) {
        Intent mStartActivity = new Intent(context, AuroraActivity.class);
        int mPendingIntentId = 1337;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }

    public static void copyToClipBoard(Context context, String dataToCopy) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Apk Url", dataToCopy);
        clipboard.setPrimaryClip(clip);
    }

    public static void toggleSoftInput(Context context, boolean show) {
        IBinder windowToken = ((AuroraActivity) context).getWindow().getDecorView().getWindowToken();
        InputMethodManager inputMethodManager = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && windowToken != null)
            if (show)
                inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            else
                inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
    }

    public static void attachSnapPager(Context context, RecyclerView recyclerView) {
        if (Util.snapPagerEnabled(context) && !Util.isLegacyCardEnabled(context)) {
            PagerSnapHelper snapHelper = new PagerSnapHelper();
            snapHelper.attachToRecyclerView(recyclerView);
        }
    }

    public static void setUpdatesInterval(Context context, int interval) {
        Intent intent = new Intent(context, UpdatesReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);

            if (interval > 0) {
                alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        Calendar.getInstance().getTimeInMillis(),
                        interval,
                        pendingIntent
                );
            }
        }
        Log.i("Periodic update preferences updated");
    }

    public static void clearOldInstallationSessions(Context context) {
        final PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        for (PackageInstaller.SessionInfo sessionInfo : packageInstaller.getMySessions()) {
            final int sessionId = sessionInfo.getSessionId();
            try {
                packageInstaller.abandonSession(sessionInfo.getSessionId());
                Log.i("Abandoned session id -> %d", sessionId);
            } catch (Exception e) {
                Log.e(e.getMessage());
            }
        }
    }

    public static boolean shouldCheckUpdate(Context context) {
        try {
            long lastSyncDate = Long.parseLong(PrefUtil.getString(context, Constants.PREFERENCE_SELF_UPDATE_DATE));
            long currentSyncDate = Calendar.getInstance().getTimeInMillis();
            long diffDatesInMillis = currentSyncDate - lastSyncDate;
            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffDatesInMillis);
            return diffInDays >= 1;
        } catch (Exception e) {
            return true;
        }
    }

    public static void setSelfUpdateTime(Context context, Long dateInMillis) {
        PrefUtil.putString(context, Constants.PREFERENCE_SELF_UPDATE_DATE, String.valueOf(dateInMillis));
    }
}
