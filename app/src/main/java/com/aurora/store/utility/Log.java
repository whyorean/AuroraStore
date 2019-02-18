package com.aurora.store.utility;

import com.aurora.store.Constants;

public class Log {

    public static void e(String message, Object... args) {
        e(String.format(message, args));
    }

    public static void e(String message) {
        android.util.Log.e(Constants.TAG, message);
    }

    public static void i(String message, Object... args) {
        i(String.format(message, args));
    }

    public static void i(String message) {
        android.util.Log.i(Constants.TAG, message);
    }

    public static void d(String message, Object... args) {
        d(String.format(message, args));
    }

    public static void d(String message) {
        android.util.Log.d(Constants.TAG, message);
    }

    public static void w(String message, Object... args) {
        w(String.format(message, args));
    }

    public static void w(String message) {
        android.util.Log.w(Constants.TAG, message);
    }
}
