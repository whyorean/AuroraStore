package com.aurora.store.utility;

import android.content.Context;

import androidx.core.app.NotificationCompat;

import com.aurora.store.Constants;
import com.aurora.store.NotificationProvider;

import java.util.Map;

public class NotificationUtil {

    private static final String PSEUDO_NOTIFICATION_MAP = "PSEUDO_NOTIFICATION_MAP";

    public static boolean isNotificationEnabled(Context context) {
        return Util.getPrefs(context).getBoolean(Constants.PREFERENCE_NOTIFICATION_TOGGLE, true);
    }

    public static int getNotificationPriority(Context context) {
        String prefValue = Util.getPrefs(context).getString(Constants.PREFERENCE_NOTIFICATION_PRIORITY, "");
        switch (prefValue) {
            case "0":
                return NotificationCompat.PRIORITY_DEFAULT;
            case "1":
                return NotificationCompat.PRIORITY_HIGH;
            case "2":
                return NotificationCompat.PRIORITY_MAX;
            default:
                return NotificationCompat.PRIORITY_DEFAULT;
        }
    }

    public static NotificationProvider getNotificationProvider(Context context) {
        String prefValue = Util.getPrefs(context).getString(Constants.PREFERENCE_NOTIFICATION_PROVIDER, "");
        switch (prefValue) {
            case "0":
                return NotificationProvider.NATIVE;
            case "1":
                return NotificationProvider.AURORA;
            default:
                return NotificationProvider.AURORA;
        }
    }

    public static Boolean shouldNotify(Context context, String packageName) {
        Map<String, String> pseudoMap = getDNDNotificationMap(context);
        String value = TextUtil.emptyIfNull(pseudoMap.get(packageName));
        return !value.equals("DND");
    }

    private static Map<String, String> getDNDNotificationMap(Context context) {
        return PrefUtil.getMap(context, PSEUDO_NOTIFICATION_MAP);
    }

    public static void updateDNDNotificationMap(Context context, String packageName, String value) {
        Map<String, String> pseudoMap = getDNDNotificationMap(context);
        pseudoMap.put(packageName, value);
        PrefUtil.saveMap(context, pseudoMap, PSEUDO_NOTIFICATION_MAP);
    }
}
