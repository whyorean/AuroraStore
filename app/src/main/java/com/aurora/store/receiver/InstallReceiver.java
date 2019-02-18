package com.aurora.store.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.aurora.store.installer.Installer;

import static com.aurora.store.notification.GeneralNotification.INTENT_APP_VERSION;
import static com.aurora.store.notification.GeneralNotification.INTENT_PACKAGE_NAME;

public class InstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if ((extras != null)) {
            final String packageName = extras.getString(INTENT_PACKAGE_NAME, "");
            final int appVersion = extras.getInt(INTENT_APP_VERSION, -1);
            if (!packageName.isEmpty() && appVersion != -1) {
                new Installer(context).installSplit(packageName, appVersion);
            }
        }
    }
}
