package com.aurora.store.data.service

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.aurora.Constants
import com.aurora.extensions.isNAndAbove
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.File
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.downloader.RequestBuilder.buildRequest
import com.aurora.store.data.downloader.getGroupId
import com.aurora.store.data.installer.NativeInstaller
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.util.CertUtil.isFDroidApp
import com.aurora.store.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tonyodev.fetch2.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Modifier
import java.util.*

class SelfUpdateService : LifecycleService() {
    private lateinit var app: App
    private lateinit var fetch: Fetch
    private lateinit var fetchListener: FetchListener

    private var gson: Gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
        .create()

    private val hashCode = BuildConfig.APPLICATION_ID.hashCode()

    // Coroutine
    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val rawSelfUpdate = intent?.getStringExtra(Constants.STRING_EXTRA)
        if (rawSelfUpdate?.isNotBlank() == true) {
            val selfUpdate = gson.fromJson(rawSelfUpdate, SelfUpdate::class.java)
            selfUpdate?.let {
                downloadAndUpdate(it)
            }
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1, notification)
        } else {
            val notification = getNotification(
                NotificationCompat.Builder(
                    this,
                    Constants.NOTIFICATION_CHANNEL_GENERAL
                )
            )
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    private fun destroyService() {
        Log.d("Self-update service destroyed")
        fetch.removeListener(fetchListener)
        if (isNAndAbove()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    private fun downloadAndUpdate(update: SelfUpdate) {
        app = App(BuildConfig.APPLICATION_ID)
        app.id = hashCode
        app.packageName = BuildConfig.APPLICATION_ID
        app.displayName = getString(R.string.app_name)
        app.versionName = update.versionName
        app.versionCode = update.versionCode

        val file = File().apply {
            name = "AuroraStore.apk"
            url = if (isFDroidVariant)
                update.fdroidBuild
            else
                update.auroraBuild
            type = File.FileType.BASE
        }

        val request = buildRequest(this, app, file)
        request.enqueueAction = EnqueueAction.REPLACE_EXISTING

        val requestList: MutableList<Request> = ArrayList()
        requestList.add(request)

        fetch = DownloadManager.with(this).fetch
        fetch.enqueue(requestList) {
            Log.i("Downloading latest update")
        }

        fetchListener = getFetchListener()
        fetch.addListener(fetchListener)
    }

    private val isFDroidVariant: Boolean
        get() = isFDroidApp(this, BuildConfig.APPLICATION_ID)

    @get:RequiresApi(Build.VERSION_CODES.O)
    private val notification: Notification
        get() {
            val notificationBuilder =
                NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_GENERAL)
            return getNotification(notificationBuilder)
        }

    private fun getNotification(builder: NotificationCompat.Builder): Notification {
        return builder.setAutoCancel(true)
            .setColor(ContextCompat.getColor(this, R.color.colorAccent))
            .setContentTitle("Self update")
            .setContentText("Updating Aurora Store in background")
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_notification_outlined)
            .build()
    }

    private fun getFetchListener(): FetchListener {
        return object : AbstractFetchGroupListener() {
            override fun onError(
                groupId: Int, download: Download, error: Error,
                throwable: Throwable?, fetchGroup: FetchGroup
            ) {
                if (groupId == app.getGroupId(this@SelfUpdateService)) {
                    Log.e("Error self-updating ${app.displayName}")
                    destroyService()
                }
            }

            override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.getGroupId(this@SelfUpdateService) && fetchGroup.groupDownloadProgress == 100) {
                    Log.d("Calling installer ${app.displayName}")

                    try {
                        NativeInstaller(this@SelfUpdateService).install(
                            app.packageName,
                            fetchGroup.downloads.map { it.file }
                        )
                    } catch (e: Exception) {
                        Log.e("Self update : ${e.stackTraceToString()}")
                    }

                    serviceScope.launch {
                        delay(10000)
                        destroyService()
                    }
                }
            }

            override fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.getGroupId(this@SelfUpdateService)) {
                    Log.d("Self-update cancelled ${app.displayName}")
                    destroyService()
                }
            }
        }
    }
}