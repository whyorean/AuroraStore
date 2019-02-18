package com.aurora.store.utility;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class ContextUtil {

    public static void toast(Context context, int stringId, String... params) {
        toastLong(context, context.getString(stringId, (Object[]) params));
    }

    public static void toastShort(final Context context, final String message) {
        runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public static void toastLong(final Context context, final String message) {
        runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    public static void runOnUiThread(final Runnable action) {
        if (isUiThread()) {
            action.run();
        } else {
            new Handler(Looper.getMainLooper()).post(action::run);
        }
    }

    public static boolean isUiThread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? Looper.getMainLooper().isCurrentThread()
                : Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    public static boolean isAlive(Context context) {
        if (!(context instanceof Activity)) {
            return false;
        }
        Activity activity = (Activity) context;
        return !activity.isDestroyed();
    }
}
