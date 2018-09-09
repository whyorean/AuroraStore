package com.dragons.aurora;

import android.os.Environment;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static com.dragons.aurora.Aurora.TAG;

public class AuroraLogTree extends Timber.DebugTree {

    /* Priority Order : Err > Debug > Warn >Info */
    private static String colorBackground[] = {"#F85441", "#FCD230", "#4768FD", "#31E7B6"};

    private static int getPriority(int priority) {
        switch (priority) {
            case ERROR:
                return 0;
            case DEBUG:
                return 1;
            case WARN:
                return 2;
            case INFO:
                return 3;
            default:
                return 3;
        }
    }

    @Override
    protected void log(int priority, String tag, @NotNull String message, Throwable t) {
        try {
            File directory = new File(Environment.getExternalStorageDirectory() + File.separator + Aurora.TAG);
            if (!directory.exists())
                directory.mkdir();

            String fileNameTimeStamp = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            String logTimeStamp = new SimpleDateFormat("E MMM dd yyyy 'at' hh:mm:ss:SSS aaa", Locale.getDefault()).format(new Date());
            String fileExt = fileNameTimeStamp + ".html";

            File file = new File(Environment.getExternalStorageDirectory() + File.separator + Aurora.TAG + File.separator + fileExt);
            file.createNewFile();

            if (file.exists()) {
                OutputStream fileOutputStream = new FileOutputStream(file, true);
                fileOutputStream.write((
                        "<p style=\"background:lightgray; color:white;\">" +
                                "<strong style=\"background:" + colorBackground[getPriority(priority)] + "; color:white;\">" + "&nbsp&nbsp" + logTimeStamp + "&nbsp&nbsp" + "</strong>" +
                                "&nbsp&nbsp" + message + "&nbsp&nbsp" + "</p>").getBytes());
                fileOutputStream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
