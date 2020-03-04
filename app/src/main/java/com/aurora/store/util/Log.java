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

package com.aurora.store.util;

import android.content.Context;
import android.os.Environment;

import com.aurora.store.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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

    public static void writeToFile(Context context, Object object) {
        try {
            FileWriter out = new FileWriter(new File(context.getFilesDir(), "AuroraLogs.txt"));
            out.write(object.toString());
            out.close();
        } catch (IOException e) {
            Log.e(e.getMessage());
        }
    }

    public static void writeLogFile(Object object) {
        try {
            FileWriter out = new FileWriter(new File(Environment.getExternalStorageDirectory().getPath(), "Aurora/Logcat.txt"));
            out.write(object.toString());
            out.close();
        } catch (IOException e) {
            Log.e(e.getMessage());
        }
    }
}
