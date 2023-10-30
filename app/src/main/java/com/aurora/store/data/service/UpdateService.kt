package com.aurora.store.data.service

import android.app.Notification
import android.content.Intent
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.aurora.Constants
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.stackTraceToString
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.exceptions.ApiException
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.R
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.downloader.RequestBuilder
import com.aurora.store.data.downloader.getGroupId
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.UpdateFile
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.util.Log
import com.aurora.store.util.PackageUtil.isSharedLibraryInstalled
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.Reason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.timerTask

class UpdateService: LifecycleService() {

    lateinit var fetch: Fetch
    lateinit var downloadManager: DownloadManager
    private lateinit var fetchListener: FetchGroupListener
    private var fetchActiveDownloadObserver = object : FetchObserver<Boolean> {
        override fun onChanged(data: Boolean, reason: Reason) {
            if (!data && isEmptyInstalling() && fetchListeners.isEmpty() && appMetadataListeners.isEmpty()) {
                Handler(Looper.getMainLooper()).postDelayed ({
                    if (isEmptyInstalling() && fetchListeners.isEmpty() && appMetadataListeners.isEmpty()) {
                        ServiceCompat.stopForeground(
                            this@UpdateService,
                            ServiceCompat.STOP_FOREGROUND_REMOVE
                        )
                        stopSelf()
                    }
                }, 5 * 1000)
            }
        }
    }
    private var hasActiveDownloadObserver = false

    private val fetchListeners: ArrayList<FetchGroupListener> = ArrayList()

    private val fetchPendingEvents: MutableMap</*groupId: */Int, AppDownloadStatus> = mutableMapOf()

    private val appMetadataListeners: ArrayList<AppMetadataStatusListener> = ArrayList()

    private val appMetadataPendingEvents: ArrayList<AppMetadataStatus> = ArrayList()

    var liveUpdateData: MutableLiveData<MutableMap<Int, UpdateFile>> = MutableLiveData()

    inner class UpdateServiceBinder : Binder() {
        fun getUpdateService() : UpdateService {
            return this@UpdateService
        }
    }

    // HashMap<packageName, HashSet<downloadFilePath>>
    private var downloadsInCompletedGroup = HashMap<String, HashSet<String>>()

    private var installing = HashSet<String>()
    private var lock = ReentrantLock()

    // Coroutine
    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    fun putInInstalling(packageName: String?) {
        if (packageName == null) {
            return
        }
        if (lock.tryLock()) {
            installing.add(packageName)
            try {
                lock.unlock()
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        } else {
            Thread {
                while (!lock.tryLock()) {
                    Thread.sleep(50)
                }
                installing.add(packageName)
                try {
                    lock.unlock()
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }.start()
        }
    }

    fun removeFromInstalling(packageName: String?, runFromCurrentThread: Boolean = false) {
        if (packageName == null) {
            return
        }
        if (lock.tryLock()) {
            installing.remove(packageName)
            try {
                lock.unlock()
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        } else {
            val toRun = Runnable {
                while (!lock.tryLock()) {
                    Thread.sleep(50)
                }
                installing.remove(packageName)
                try {
                    lock.unlock()
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }
            if (runFromCurrentThread) {
                toRun.run()
            } else {
                Thread(toRun).start()
            }
        }
    }

    fun isEmptyInstalling(): Boolean {
        while (!lock.tryLock()) {
            Thread.sleep(50)
        }
        val out: Boolean = installing.isEmpty()
        try {
            lock.unlock()
        } catch (th: Throwable) {
            th.printStackTrace()
        }
        return out
    }

    fun containsInInstalling(packageName: String?): Boolean {
        if (packageName == null) {
            return false
        }
        while (!lock.tryLock()) {
            Thread.sleep(50)
        }
        val out = installing.contains(packageName)
        try {
            lock.unlock()
        } catch (th: Throwable) {
            th.printStackTrace()
        }
        return out
    }


    private lateinit var purchaseHelper: PurchaseHelper

    private lateinit var authData: AuthData


    data class AppDownloadStatus(val download: Download, val fetchGroup: FetchGroup,
                                 val isCancelled: Boolean = false,
                                 val isComplete: Boolean = false)

    data class AppMetadataStatus(val reason: String, val app: App)

    @get:RequiresApi(Build.VERSION_CODES.O)
    private val notification: Notification
        get() {
            val notificationBuilder =
                NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_UPDATER_SERVICE)
            return getNotification(notificationBuilder)
        }

    private fun getNotification(builder: NotificationCompat.Builder): Notification {
        return builder.setAutoCancel(true)
            .setColor(ContextCompat.getColor(this, R.color.colorAccent))
            .setContentTitle(getString(R.string.app_updater_service_notif_title))
            .setContentText(getString(R.string.app_updater_service_notif_text))
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
                    Constants.NOTIFICATION_CHANNEL_UPDATER_SERVICE
                )
            )
            startForeground(1, notification)
        }
        EventBus.getDefault().register(this)
        authData = AuthProvider.with(this).getAuthData()
        purchaseHelper = PurchaseHelper(authData).using(HttpClient.getPreferredClient(this))
        downloadManager = DownloadManager.with(this)
        fetch = downloadManager.fetch
        fetchListener = object : FetchGroupListener {

            override fun onAdded(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                fetchListeners.forEach {
                    it.onAdded(groupId, download, fetchGroup)
                }
                if (download.tag != null) {
                    removeFromInstalling(download.tag, runFromCurrentThread = true)
                }
                if (!hasActiveDownloadObserver) {
                    hasActiveDownloadObserver = true
                    fetch.addActiveDownloadsObserver(true, fetchActiveDownloadObserver)
                }
            }

            override fun onAdded(download: Download) {
                fetchListeners.forEach {
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
                fetchListeners.forEach {
                    it.onProgress(groupId, download, etaInMilliSeconds, downloadedBytesPerSecond, fetchGroup)
                }
            }

            override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
                fetchListeners.forEach {
                    it.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)
                }
            }

            override fun onQueued(groupId: Int, download: Download, waitingNetwork: Boolean, fetchGroup: FetchGroup) {
                fetchListeners.forEach {
                    it.onQueued(groupId, download, waitingNetwork, fetchGroup)
                }
            }

            override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
                fetchListeners.forEach {
                    it.onQueued(download, waitingOnNetwork)
                }
            }

            override fun onRemoved(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                fetchListeners.forEach {
                    it.onRemoved(groupId, download, fetchGroup)
                }
            }

            override fun onRemoved(download: Download) {
                fetchListeners.forEach {
                    it.onRemoved(download)
                }
            }

            override fun onResumed(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                fetchListeners.forEach {
                    it.onResumed(groupId, download, fetchGroup)
                }
            }

            override fun onResumed(download: Download) {
                fetchListeners.forEach {
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
                fetchListeners.forEach {
                    it.onStarted(
                        groupId,
                        download,
                        downloadBlocks,
                        totalBlocks,
                        fetchGroup)
                }
            }

            override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
                fetchListeners.forEach {
                    it.onStarted(
                        download,
                        downloadBlocks,
                        totalBlocks)
                }
            }

            override fun onWaitingNetwork(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                fetchListeners.forEach {
                    it.onWaitingNetwork(groupId, download, fetchGroup)
                }
            }

            override fun onWaitingNetwork(download: Download) {
                fetchListeners.forEach {
                    it.onWaitingNetwork(download)
                }
            }

            override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                fetchListeners.forEach {
                    it.onCompleted(groupId, download, fetchGroup)
                }
                if (fetchListeners.isEmpty()) {
                    fetchPendingEvents[groupId] = AppDownloadStatus(download, fetchGroup, isComplete = true)
                }
                var packageDownloadFilesWhichCompleted = downloadsInCompletedGroup[download.tag!!]
                if (packageDownloadFilesWhichCompleted == null) {
                    packageDownloadFilesWhichCompleted = HashSet()
                    downloadsInCompletedGroup[download.tag!!] = packageDownloadFilesWhichCompleted
                }
                packageDownloadFilesWhichCompleted.add(download.file)
                if (fetchGroup.groupDownloadProgress == 100 && fetchGroup.downloads.all { packageDownloadFilesWhichCompleted.contains(it.file) }) {
                    downloadsInCompletedGroup.remove(download.tag!!)
                    if (download.tag != null) {
                        removeFromInstalling(download.tag, runFromCurrentThread = true)
                    }
                    Log.d("Group (${download.tag!!}) downloaded and verified all downloaded!")
                    Handler(Looper.getMainLooper()).post {
                        try {
                            install(download.tag!!, fetchGroup.downloads)
                        } catch (e: Exception) {
                            Log.e(e.stackTraceToString())
                        }
                    }
                }
                if (fetchGroup.groupDownloadProgress == 100) {
                    Log.d("Group (${download.tag!!}) downloaded but NOT verified all downloaded!")
                } /* else if (fetchGroup.groupDownloadProgress == -1) {
                    fetch.deleteGroup(fetchGroup.id)
                }*/
            }

            override fun onCompleted(download: Download) {
                fetchListeners.forEach {
                    it.onCompleted(download)
                }
            }

            override fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                fetchListeners.forEach {
                    it.onCancelled(groupId, download, fetchGroup)
                }
                if (fetchListeners.isEmpty()) {
                    fetchPendingEvents[groupId] = AppDownloadStatus(download, fetchGroup, isCancelled = true)
                }
            }

            override fun onCancelled(download: Download) {
                fetchListeners.forEach {
                    it.onCancelled(download)
                }
            }

            override fun onDeleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                fetchListeners.forEach {
                    it.onDeleted(groupId, download, fetchGroup)
                }
                if (fetchListeners.isEmpty()) {
                    fetchPendingEvents[groupId] = AppDownloadStatus(download, fetchGroup, isCancelled = true)
                }
            }

            override fun onDeleted(download: Download) {
                fetchListeners.forEach {
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
                fetchListeners.forEach {
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
                fetchListeners.forEach {
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
                fetchListeners.forEach {
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
                fetchListeners.forEach {
                    it.onError(
                        download,
                        error,
                        throwable
                    )
                }
            }

            override fun onPaused(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                fetchListeners.forEach {
                    it.onPaused(groupId, download, fetchGroup)
                }
            }

            override fun onPaused(download: Download) {
                fetchListeners.forEach {
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

    fun updateApp(app: App, removeExisiting: Boolean = false) {
        putInInstalling(app.packageName)
        serviceScope.launch {
            try {
                val files = purchaseHelper.purchase(
                    app.packageName,
                    app.versionCode,
                    app.offerType
                )

                if (app.dependencies.dependentLibraries.isNotEmpty() && isOAndAbove()) {
                    app.dependencies.dependentLibraries.forEach {
                        if (!isSharedLibraryInstalled(
                                this@UpdateService,
                                it.packageName,
                                it.versionCode
                            )
                        ) {
                            it.displayName = getString(R.string.downloading_dep, app.displayName)
                            it.iconArtwork = app.iconArtwork

                            updateApp(it, removeExisiting)
                            while (containsInInstalling(it.packageName) ||
                                !isSharedLibraryInstalled(
                                    this@UpdateService,
                                    it.packageName,
                                    it.versionCode
                                )
                            ) {
                                delay(1000)
                            }

                            // Clear library downloads
                            clearDownloadsIfRequired(it.packageName)
                        }
                    }
                }

                val requests = files.filter { it.url.isNotEmpty() }
                    .map { RequestBuilder.buildRequest(this@UpdateService, app, it) }
                    .toList()

                if (requests.isNotEmpty()) {
                    if (removeExisiting) {
                        /*Remove old fetch group if downloaded earlier, mostly in case of updates*/
                        fetch.deleteGroup(app.getGroupId(this@UpdateService))
                    }
                    fetch.enqueue(requests) {
                        Log.i("Updating ${app.displayName}")
                    }
                } else {
                    removeFromInstalling(app.packageName)
                    Log.e("Failed to download : ${app.displayName}")
                    appMetadataListeners.forEach {
                        it.onAppMetadataStatusError(
                            getString(R.string.purchase_session_expired),
                            app
                        )
                    }

                    if (appMetadataListeners.isEmpty()) {
                        appMetadataPendingEvents.add(
                            AppMetadataStatus(
                                getString(R.string.purchase_session_expired),
                                app
                            )
                        )
                    }
                }
            } catch (exception: Exception) {
                removeFromInstalling(app.packageName)
                var reason = "Unknown"

                when (exception) {
                    is ApiException.AppNotPurchased -> {
                        reason = getString(R.string.purchase_invalid)
                    }

                    is ApiException.AppNotFound -> {
                        reason = getString(R.string.purchase_not_found)
                    }

                    is ApiException.AppNotSupported -> {
                        reason = getString(R.string.purchase_unsupported)
                    }

                    is ApiException.EmptyDownloads -> {
                        reason = getString(R.string.purchase_no_file)
                    }
                }

                appMetadataListeners.forEach {
                    it.onAppMetadataStatusError(reason, app)
                }

                if (appMetadataListeners.isEmpty()) {
                    appMetadataPendingEvents.add(AppMetadataStatus(reason, app))
                }

                Log.e("Failed to purchase ${app.displayName} : $reason")
            }
        }
    }

    var timer: Timer? = null
    val timerTaskRun: Runnable = Runnable {
        Handler(Looper.getMainLooper()).post {
            if (isEmptyInstalling() && fetchListeners.isEmpty() && appMetadataListeners.isEmpty()) {
                fetch.hasActiveDownloads(true) { hasActiveDownloads ->
                    if (!hasActiveDownloads && isEmptyInstalling() && fetchListeners.isEmpty() && appMetadataListeners.isEmpty()) {
                        Handler(Looper.getMainLooper()).post {
                            ServiceCompat.stopForeground(
                                this@UpdateService,
                                ServiceCompat.STOP_FOREGROUND_REMOVE
                            )
                            stopSelf()
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    private fun install(packageName: String, files: List<Download>?) {
        if (containsInInstalling(packageName)) {
            println("Already installing $packageName!")
            return
        }
        putInInstalling(packageName)
        files?.let { downloads ->
            if (downloads.all { File(it.file).exists() }) {
                serviceScope.launch {
                    try {
                        AppInstaller.getInstance(this@UpdateService).getPreferredInstaller()
                            .install(
                                packageName,
                                files.filter { it.file.endsWith(".apk") }.map { it.file }.toList()
                            )
                    } catch (exception: Exception) {
                        removeFromInstalling(packageName)
                        Log.e(exception.stackTraceToString())
                    }
                }
            } else {
                removeFromInstalling(packageName)
            }
        }
    }

    var timerLock = Object()

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEventBackgroundThreadExec(event: Any) {
        when (event) {
            is InstallerEvent.Success,
            is InstallerEvent.Cancelled,
            is InstallerEvent.Failed -> {
                when (event) {
                    is InstallerEvent.Success -> event.packageName
                    is InstallerEvent.Cancelled -> event.packageName
                    is InstallerEvent.Failed -> event.packageName
                    else -> null
                }?.run {
                    removeFromInstalling(this)
                }
                synchronized(timerLock) {
                    if (timer != null) {
                        timer!!.cancel()
                        timer = null
                    }
                    if (timer == null) {
                        timer = Timer()
                    }
                    timer!!.schedule(timerTask { timerTaskRun.run() }, 5 * 1000)
                }
            }
            else -> {

            }
        }
    }

    fun registerFetchListener(listener: FetchGroupListener) {
        fetchListeners.add(listener)
        val iterator = fetchPendingEvents.iterator()
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

    fun registerAppMetadataListener(listener: AppMetadataStatusListener) {
        appMetadataListeners.add(listener)
        val iterator = appMetadataPendingEvents.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            listener.onAppMetadataStatusError(item.reason, item.app)
            iterator.remove()
        }
    }

    fun unregisterAppMetadataListener(listener: AppMetadataStatusListener) {
        appMetadataListeners.remove(listener)
    }

    fun unregisterFetchListener(listener: AbstractFetchGroupListener) {
        fetchListeners.remove(listener)
    }

    private var binder: UpdateServiceBinder = UpdateServiceBinder()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        fetchListeners.clear()
        appMetadataListeners.clear()
        synchronized(timerLock) {
            if (timer != null) {
                timer!!.cancel()
                timer = null
            }
            if (timer == null) {
                timer = Timer()
            }
            timer!!.schedule(timerTask { timerTaskRun.run() }, 5 * 1000)
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

    private fun clearDownloadsIfRequired(packageName: String) {
        if (Preferences.getBoolean(this, Preferences.PREFERENCE_AUTO_DELETE)) {
            try {
                val rootDirPath = PathUtil.getPackageDirectory(this, packageName)
                val rootDir = File(rootDirPath)
                if (rootDir.exists()) rootDir.deleteRecursively()
            } catch (e: Exception) {
                Log.d("Failed to clear downloads!", e)
            }
        }
    }
}
