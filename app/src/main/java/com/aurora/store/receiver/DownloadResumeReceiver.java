package com.aurora.store.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.aurora.store.download.DownloadManager;

import static com.aurora.store.notification.GeneralNotification.REQUEST_ID;

public class DownloadResumeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if ((extras != null)) {
            final int requestId = extras.getInt(REQUEST_ID, -1);
            final DownloadManager mDownloadManager = new DownloadManager(context);
            mDownloadManager.getFetchInstance().resume(requestId);
        }
    }
}
