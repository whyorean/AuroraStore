package com.aurora.store.data.service

import android.app.Notification
import android.content.Intent
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.aurora.Constants
import com.aurora.extensions.stackTraceToString
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.R
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.downloader.RequestBuilder
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.UpdateFile
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.util.Log
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.Reason
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timerTask

class UpdateService: LifecycleService() {

    lateinit var fetch: Fetch
    lateinit var downloadManager: DownloadManager
    private lateinit var fetchListener: FetchGroupListener
    private var fetchActiveDownloadObserver = object : FetchObserver<Boolean> {
        override fun onChanged(data: Boolean, reason: Reason) {
            if (!data && !installing.get() && listeners.isEmpty()) {
                Handler(Looper.getMainLooper()).postDelayed ({
                    if (!installing.get() && listeners.isEmpty()) {
                        stopSelf()
                    }
                }, 5 * 1000)
            }
        }
    }
    private var hasActiveDownloadObserver = false

    private val listeners: ArrayList<FetchGroupListener> = ArrayList()

    private val pendingEvents: MutableMap</*groupId: */Int, AppDownloadStatus> = mutableMapOf()

    var liveUpdateData: MutableLiveData<MutableMap<Int, UpdateFile>> = MutableLiveData()

    inner class UpdateServiceBinder : Binder() {
        fun getUpdateService() : UpdateService {
            return this@UpdateService
        }
    }

    private var installing: AtomicBoolean = AtomicBoolean()


    private lateinit var purchaseHelper: PurchaseHelper

    private lateinit var authData: AuthData


    data class AppDownloadStatus(val download: Download, val fetchGroup: FetchGroup,
                                 val isCancelled: Boolean = false,
                                 val isComplete: Boolean = false)

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
            .setContentTitle("Updating apps")
            .setContentText("Updating apps in the background")
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_notification_outlined)
            .build()
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
        EventBus.getDefault().register(this)
        authData = AuthProvider.with(this).getAuthData()
        purchaseHelper = PurchaseHelper(authData)
        downloadManager = DownloadManager.with(this)
        fetch = downloadManager.fetch
        fetchListener = object : FetchGroupListener {

            override fun onAdded(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                listeners.forEach {
                    it.onAdded(groupId, download, fetchGroup)
                }
                if (!hasActiveDownloadObserver) {
                    hasActiveDownloadObserver = true
                    fetch.addActiveDownloadsObserver(true, fetchActiveDownloadObserver)
                }
            }

            override fun onAdded(download: Download) {
                listeners.forEach {
                    it.onAdded(download)
                }
            }

            override fun onProgress(
                groupId: Int,
                download: Download,
                etaInMilliSeconds: Long,
                downloadedBytesPerSecond: Long,
                fetchGroup: FetchGroup
            ) {
                listeners.forEach {
                    it.onProgress(groupId, download, etaInMilliSeconds, downloadedBytesPerSecond, fetchGroup)
                }
            }

            override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
                listeners.forEach {
                    it.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)
                }
            }

            override fun onQueued(groupId: Int, download: Download, waitingNetwork: Boolean, fetchGroup: FetchGroup) {
                listeners.forEach {
                    it.onQueued(groupId, download, waitingNetwork, fetchGroup)
                }
            }

            override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
                listeners.forEach {
                    it.onQueued(download, waitingOnNetwork)
                }
            }

            override fun onRemoved(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                listeners.forEach {
                    it.onRemoved(groupId, download, fetchGroup)
                }
            }

            override fun onRemoved(download: Download) {
                listeners.forEach {
                    it.onRemoved(download)
                }
            }

            override fun onResumed(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                listeners.forEach {
                    it.onResumed(groupId, download, fetchGroup)
                }
            }

            override fun onResumed(download: Download) {
                listeners.forEach {
                    it.onResumed(download)
                }
            }

            override fun onStarted(
                groupId: Int,
                download: Download,
                downloadBlocks: List<DownloadBlock>,
                totalBlocks: Int,
                fetchGroup: FetchGroup
            ) {
                listeners.forEach {
                    it.onStarted(
                        groupId,
                        download,
                        downloadBlocks,
                        totalBlocks,
                        fetchGroup)
                }
            }

            override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
                listeners.forEach {
                    it.onStarted(
                        download,
                        downloadBlocks,
                        totalBlocks)
                }
            }

            override fun onWaitingNetwork(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                listeners.forEach {
                    it.onWaitingNetwork(groupId, download, fetchGroup)
                }
            }

            override fun onWaitingNetwork(download: Download) {
                listeners.forEach {
                    it.onWaitingNetwork(download)
                }
            }

            override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                listeners.forEach {
                    it.onCompleted(groupId, download, fetchGroup)
                }
                if (listeners.isEmpty()) {
                    pendingEvents[groupId] = AppDownloadStatus(download, fetchGroup, isComplete = true)
                }
                if (fetchGroup.groupDownloadProgress == 100) {
                    Handler(Looper.getMainLooper()).post {
                        try {
                            install(download.tag!!, fetchGroup.downloads)
                        } catch (e: Exception) {
                            Log.e(e.stackTraceToString())
                        }
                    }
                } /* else if (fetchGroup.groupDownloadProgress == -1) {
                    fetch.deleteGroup(fetchGroup.id)
                }*/
            }

            override fun onCompleted(download: Download) {
                listeners.forEach {
                    it.onCompleted(download)
                }
            }

            override fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                listeners.forEach {
                    it.onCancelled(groupId, download, fetchGroup)
                }
                if (listeners.isEmpty()) {
                    pendingEvents[groupId] = AppDownloadStatus(download, fetchGroup, isCancelled = true)
                }
            }

            override fun onCancelled(download: Download) {
                TODO("Not yet implemented")
            }

            override fun onDeleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                listeners.forEach {
                    it.onDeleted(groupId, download, fetchGroup)
                }
                if (listeners.isEmpty()) {
                    pendingEvents[groupId] = AppDownloadStatus(download, fetchGroup, isCancelled = true)
                }
            }

            override fun onDeleted(download: Download) {
                listeners.forEach {
                    it.onDeleted(download)
                }
            }

            override fun onDownloadBlockUpdated(
                groupId: Int,
                download: Download,
                downloadBlock: DownloadBlock,
                totalBlocks: Int,
                fetchGroup: FetchGroup
            ) {
                listeners.forEach {
                    it.onDownloadBlockUpdated(
                        groupId,
                        download,
                        downloadBlock,
                        totalBlocks,
                        fetchGroup
                    )
                }
            }

            override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {
                listeners.forEach {
                    it.onDownloadBlockUpdated(
                        download,
                        downloadBlock,
                        totalBlocks
                    )
                }
            }

            override fun onError(
                groupId: Int,
                download: Download,
                error: Error,
                throwable: Throwable?,
                fetchGroup: FetchGroup
            ) {
                listeners.forEach {
                    it.onError(
                        groupId,
                        download,
                        error,
                        throwable,
                        fetchGroup
                    )
                }
            }

            override fun onError(download: Download, error: Error, throwable: Throwable?) {
                listeners.forEach {
                    it.onError(
                        download,
                        error,
                        throwable
                    )
                }
            }

            override fun onPaused(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                listeners.forEach {
                    it.onPaused(groupId, download, fetchGroup)
                }
            }

            override fun onPaused(download: Download) {
                listeners.forEach {
                    it.onPaused(download)
                }
            }
        }
        /*liveUpdateData.observe(this) { updateData ->

        }*/
        if (::fetch.isInitialized && ::fetchListener.isInitialized) {
            fetch.addListener(fetchListener)
        }
    }

    fun updateApp(app: App) {
        installing.set(true)
        task {
            val files = purchaseHelper.purchase(
                app.packageName,
                app.versionCode,
                app.offerType
            )

            files.map { RequestBuilder.buildRequest(this, app, it) }
        } successUi {
            val requests = it.filter { request -> request.url.isNotEmpty() }.toList()
            if (requests.isNotEmpty()) {
                fetch.enqueue(requests) {
                    Log.i("Updating ${app.displayName}")
                }
            } else {
                toast("Failed to update ${app.displayName}")
            }
        } failUi {
            Log.e("Failed to update ${app.displayName}")
        }
    }

    var timer: Timer? = null
    val timerTaskRun: Runnable = Runnable {
        Handler(Looper.getMainLooper()).post {
            if (!installing.get() && listeners.isEmpty()) {
                fetch.hasActiveDownloads(true) { hasActiveDownloads ->
                    if (!hasActiveDownloads && !installing.get() && listeners.isEmpty()) {
                        Handler(Looper.getMainLooper()).post {
                            stopSelf()
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    private fun install(packageName: String, files: List<Download>?) {
        files?.let { downloads ->
            installing.set(true)
            var filesExist = true

            downloads.forEach { download ->
                filesExist = filesExist && FileUtils.getFile(download.file).exists()
            }

            if (filesExist) {
                task {
                    try {
                        val installer = AppInstaller.getInstance(this)
                            .getPreferredInstaller()
                        installer.install(
                            packageName,
                            files
                                .filter { it.file.endsWith(".apk") }
                                .map { it.file }.toList()
                        )
                    } catch (th: Throwable) {
                        th.printStackTrace()
                    }
                }.fail {
                    Log.e(it.stackTraceToString())
                }
            }
        }
    }

    var timerLock = Object()

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEventBackgroundThreadExec(event: Any) {
        when (event) {
            is InstallerEvent.Success,
            is InstallerEvent.Failed -> {
                synchronized(timerLock) {
                    if (timer != null) {
                        timer!!.cancel()
                        timer = null
                    }
                    if (timer == null) {
                        timer = Timer()
                    }
                    installing.set(false)
                    timer!!.schedule(timerTask { timerTaskRun.run() }, 10 * 1000)
                }
            }
            else -> {

            }
        }
    }

    fun registerListener(listener: FetchGroupListener) {
        listeners.add(listener)
        val iterator = pendingEvents.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.value.isCancelled && !item.value.isComplete) {
                listener.onCancelled(item.key, item.value.download, item.value.fetchGroup)
            } else if (!item.value.isCancelled && item.value.isComplete) {
                listener.onCompleted(item.key, item.value.download, item.value.fetchGroup)
            }
            iterator.remove()
        }
    }

    fun unregisterListener(listener: AbstractFetchGroupListener) {
        listeners.remove(listener)
    }

    private var binder: UpdateServiceBinder = UpdateServiceBinder()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        listeners.clear()
        if (!installing.get()) {
            fetch.hasActiveDownloads(true, { hasActiveDownloads ->
                if (!hasActiveDownloads && !installing.get() && listeners.isEmpty()) {
                    Handler(Looper.getMainLooper()).post {
                        stopSelf()
                    }
                }
            })
        }
        return true
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (hasActiveDownloadObserver) {
            hasActiveDownloadObserver = false
            fetch.removeActiveDownloadsObserver(fetchActiveDownloadObserver)
        }
        if (::fetch.isInitialized && ::fetchListener.isInitialized) {
            fetch.removeListener(fetchListener)
        }
    }

    fun updateAll(updateFileMap: MutableMap<Int, UpdateFile>) {
        updateFileMap.values.forEach { updateApp(it.app) }
    }
}