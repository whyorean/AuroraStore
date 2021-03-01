package com.aurora.store.data.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.File
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.downloader.RequestBuilder.buildRequest
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.util.CertUtil.isFDroidApp
import com.aurora.store.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tonyodev.fetch2.*
import nl.komponents.kovenant.task
import org.apache.commons.lang3.StringUtils
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.TimeUnit

class SelfUpdateService : Service() {
    private lateinit var app: App
    private lateinit var fetch: Fetch
    private lateinit var fetchListener: FetchListener

    private var gson: Gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
        .create()

    private val hashCode = BuildConfig.APPLICATION_ID.hashCode()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val rawSelfUpdate = intent.getStringExtra(Constants.STRING_EXTRA)
        if (StringUtils.isNotEmpty(rawSelfUpdate)) {
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

    private fun destroyService() {
        Log.d("Self-update service destroyed")
        fetch.removeListener(fetchListener)
        stopForeground(true)
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
                if (groupId == app.id) {
                    Log.e("Error self-updating ${app.displayName}")
                    destroyService()
                }
            }

            override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.id && fetchGroup.groupDownloadProgress == 100) {
                    Log.d("Calling installer ${app.displayName}")
                    AppInstaller(this@SelfUpdateService).getPreferredInstaller().install(
                        BuildConfig.APPLICATION_ID,
                        listOf(download.file)
                    )

                    task {
                        TimeUnit.SECONDS.sleep(10)
                    } success {
                        destroyService()
                    }
                }
            }

            override fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.id) {
                    Log.d("Self-update cancelled ${app.displayName}")
                    destroyService()
                }
            }
        }
    }
}