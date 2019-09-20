package com.aurora.store.utility;

import android.content.Context;

import androidx.core.app.NotificationCompat;

import com.aurora.store.Constants;
import com.aurora.store.NotificationProvider;

import java.util.Map;

public class NotificationUtil {

    public static boolean isNotificationEnabled(Context context) {
        return Util.getPrefs(context).getBoolean(Constants.PREFERENCE_NOTIFICATION_TOGGLE, true);
    }

    public static int getNotificationPriority(Context context) {
        String prefValue = Util.getPrefs(context).getString(Constants.PREFERENCE_NOTIFICATION_PRIORITY, "");
        switch (prefValue) {
            case "1":
                return NotificationCompat.PRIORITY_HIGH;
            case "2":
                return NotificationCompat.PRIORITY_MAX;
            default:
                return NotificationCompat.PRIORITY_DEFAULT;
        }
    }
}
