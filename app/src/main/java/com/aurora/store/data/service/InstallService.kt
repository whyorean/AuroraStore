package com.aurora.store.data.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class InstallService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(
            "BAZINGA",
            intent?.action.toString()
        )

        return START_STICKY
    }
}