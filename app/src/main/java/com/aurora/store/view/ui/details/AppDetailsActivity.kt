/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *  Copyright (C) 2022, The Calyx Institute
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.view.ui.details

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.aurora.Constants
import com.aurora.extensions.*
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.State
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.downloader.getGroupId
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.service.AppMetadataStatusListener
import com.aurora.store.data.service.UpdateService
import com.aurora.store.databinding.ActivityDetailsBinding
import com.aurora.store.util.*
import com.aurora.store.view.ui.sheets.InstallErrorDialogSheet
import com.aurora.store.view.ui.sheets.ManualDownloadSheet
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class AppDetailsActivity : BaseDetailsActivity() {

    private lateinit var B: ActivityDetailsBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private val startForStorageManagerResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isRAndAbove() && Environment.isExternalStorageManager()) {
                updateApp(app)
            } else {
                toast(R.string.permissions_denied)
            }
        }
    private val startForPermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) updateApp(app) else toast(R.string.permissions_denied)
        }

    private lateinit var authData: AuthData
    private lateinit var app: App
    private var fetch: Fetch? = null
    private var downloadManager: DownloadManager? = null

    private var attachToServiceCalled = false
    private var updateService: UpdateService? = null
    private var pendingAddListeners = true
    private var serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            updateService = (binder as UpdateService.UpdateServiceBinder).getUpdateService()
            if (::fetchGroupListener.isInitialized && ::appMetadataListener.isInitialized && pendingAddListeners) {
                updateService!!.registerFetchListener(fetchGroupListener)
                // appMetadataListener needs to be initialized after the fetchGroupListener
                updateService!!.registerAppMetadataListener(appMetadataListener)
                pendingAddListeners = false
            }
            if (listOfActionsWhenServiceAttaches.isNotEmpty()) {
                val iterator = listOfActionsWhenServiceAttaches.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    next.run()
                    iterator.remove()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            updateService = null
            attachToServiceCalled = false
            pendingAddListeners = true
        }
    }
    private lateinit var fetchGroupListener: FetchGroupListener
    private lateinit var appMetadataListener: AppMetadataStatusListener
    private lateinit var completionMarker: java.io.File
    private lateinit var inProgressMarker: java.io.File

    private var isExternal = false
    private var isNone = false
    private var status = Status.NONE
    private var isInstalled: Boolean = false
    private var isUpdatable: Boolean = false
    private var autoDownload: Boolean = false
    private var downloadOnly: Boolean = false

    override fun onConnected() {

    }

    override fun onDisconnected() {

    }

    override fun onReconnected() {

    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        if (autoDownload) {
            purchase()
        }
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: Any) {
        when (event) {
            is BusEvent.InstallEvent -> {
                if (app.packageName == event.packageName) {
                    attachActions()
                }
            }
            is BusEvent.UninstallEvent -> {
                if (app.packageName == event.packageName) {
                    attachActions()
                }
            }
            is BusEvent.ManualDownload -> {
                if (app.packageName == event.packageName) {
                    app.versionCode = event.versionCode
                    purchase()
                }
            }
            is InstallerEvent.Failed -> {
                if (app.packageName == event.packageName) {
                    InstallErrorDialogSheet.newInstance(
                        app,
                        event.packageName,
                        event.error,
                        event.extra
                    ).show(supportFragmentManager, "SED")
                    attachActions()
                    updateActionState(State.IDLE)
                }
            }
            else -> {

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        B = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(B.root)

        onNewIntent(intent)

        onBackPressedDispatcher.addCallback(this) {
            if (isExternal) {
                open(MainActivity::class.java, true)
            } else {
                finish()
            }
        }
    }

    override fun onResume() {
        getUpdateServiceInstance()
        checkAndSetupInstall()
        super.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.scheme != null && (intent.scheme == "market" || intent.scheme == "http" || intent.scheme == "https")) {
            val packageName = intent.data!!.getQueryParameter("id")
            val packageVersion = intent.data!!.getQueryParameter("v")
            if (packageName.isNullOrEmpty()) {
                finishAfterTransition()
            } else {
                isExternal = true
                app = App(packageName)
                if (!packageVersion.isNullOrEmpty()) {
                    app.versionCode = packageVersion.toInt()
                }
                fetchCompleteApp()
            }
        } else {
            val rawApp: String? = intent.getStringExtra(Constants.STRING_EXTRA)
            if (rawApp != null) {
                app = gson.fromJson(rawApp, App::class.java)
                isInstalled = PackageUtil.isInstalled(this, app.packageName)

                inflatePartialApp()
                fetchCompleteApp()
            } else {
                finishAfterTransition()
            }
        }
    }

    private var uninstallActionEnabled = false

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_details, menu)
        if (::app.isInitialized) {
            val installed = PackageUtil.isInstalled(this, app.packageName)
            menu?.findItem(R.id.action_uninstall)?.isVisible = installed
            uninstallActionEnabled = installed
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
            R.id.action_share -> {
                share(app)
                return true
            }
            R.id.action_uninstall -> {
                uninstallApp()
                return true
            }
            R.id.menu_download_manual -> {
                val sheet = ManualDownloadSheet.newInstance(app)
                sheet.isCancelable = false
                sheet.show(supportFragmentManager, ManualDownloadSheet.TAG)
                return true
            }
            R.id.action_playstore -> {
                browse("${Constants.SHARE_URL}${app.packageName}")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun attachToolbar() {
        setSupportActionBar(B.layoutDetailsToolbar.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.elevation = 0f
            actionBar.title = ""
        }
    }

    private fun attachActions() {
        flip(0)
        checkAndSetupInstall()
    }

    private fun updateActionState(state: State) {
        B.layoutDetailsInstall.btnDownload.updateState(state)
    }

    private fun openApp() {
        val intent = PackageUtil.getLaunchIntent(this, app.packageName)
        if (intent != null) {
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                toast("Unable to open app")
            }
        }
    }

    private fun verifyAndInstall(files: List<Download>) {
        if (downloadOnly)
            return

        var filesExist = true

        files.forEach { download ->
            filesExist = filesExist && File(download.file).exists()
        }

        if (filesExist)
            install(files)
        else
            purchase()
    }

    @Synchronized
    private fun install(files: List<Download>) {
        updateActionState(State.IDLE)

        val apkFiles = files.filter { it.file.endsWith(".apk") }
        val preferredInstaller = Preferences.getInteger(this, Preferences.PREFERENCE_INSTALLER_ID)

        if (apkFiles.size > 1 && preferredInstaller == 1) {
            showDialog(R.string.title_installer, R.string.dialog_desc_native_split)
        } else {
            task {
                AppInstaller.getInstance(this)
                    .getPreferredInstaller()
                    .install(
                        app.packageName,
                        apkFiles.map { it.file }
                    )
            } fail {
                Log.e(it.stackTraceToString())
            }

            runOnUiThread {
                B.layoutDetailsInstall.btnDownload.setText(getString(R.string.action_installing))
            }
        }
    }

    @Synchronized
    private fun uninstallApp() {
        task {
            AppInstaller.getInstance(this)
                .getPreferredInstaller()
                .uninstall(app.packageName)
        }
    }

    private fun attachWhiteListStatus() {

    }

    private fun fetchCompleteApp() {
        task {
            authData = AuthProvider.with(this).getAuthData()
            return@task AppDetailsHelper(authData)
                .using(HttpClient.getPreferredClient())
                .getAppByPackageName(app.packageName)
        } successUi {
            if (isExternal) {
                app = it
                inflatePartialApp()
            }
            inflateExtraDetails(it)
        } failUi {
            toast("Failed to fetch app details")
        }
    }

    private fun inflatePartialApp() {
        if (::app.isInitialized) {
            attachWhiteListStatus()
            attachHeader()
            attachToolbar()
            attachBottomSheet()
            attachFetch()
            attachActions()

            if (autoDownload) {
                purchase()
            }
        }
    }

    private fun attachHeader() {
        B.layoutDetailsApp.apply {
            imgIcon.load(app.iconArtwork.url) {
                placeholder(R.drawable.bg_placeholder)
                transform(RoundedCorners(32))
            }

            txtLine1.text = app.displayName
            txtLine2.text = app.developerName
            txtLine2.setOnClickListener {
                NavigationUtil.openDevAppsActivity(
                    this@AppDetailsActivity,
                    app
                )
            }
            txtLine3.text = ("${app.versionName} (${app.versionCode})")
            packageName.text = app.packageName

            val tags = mutableListOf<String>()
            if (app.isFree)
                tags.add(getString(R.string.details_free))
            else
                tags.add(getString(R.string.details_paid))

            if (app.containsAds)
                tags.add(getString(R.string.details_contains_ads))
            else
                tags.add(getString(R.string.details_no_ads))

            txtLine4.text = tags.joinToString(separator = " • ")
        }
    }

    private fun inflateExtraDetails(app: App?) {
        app?.let {
            B.viewFlipper.displayedChild = 1
            inflateAppDescription(B.layoutDetailDescription, app)
            inflateAppRatingAndReviews(B.layoutDetailsReview, app)
            inflateAppDevInfo(B.layoutDetailsDev, app)
            inflateAppPrivacy(B.layoutDetailsPrivacy, app)
            inflateAppPermission(B.layoutDetailsPermissions, app)

            if (!authData.isAnonymous) {
                app.testingProgram?.let {
                    if (it.isAvailable && it.isSubscribed) {
                        B.layoutDetailsApp.txtLine1.text = it.displayName
                    }
                }

                inflateBetaSubscription(B.layoutDetailsBeta, app)
            }

            if (Preferences.getBoolean(this, Preferences.PREFERENCE_SIMILAR)) {
                inflateAppStream(B.epoxyRecyclerStream, app)
            }
        }
    }

    @Synchronized
    private fun startDownload() {
        when (status) {
            Status.PAUSED -> {
                fetch?.resumeGroup(app.getGroupId(this@AppDetailsActivity))
            }
            Status.DOWNLOADING -> {
                flip(1)
                toast("Already downloading")
            }
            Status.COMPLETED -> {
                fetch?.getFetchGroup(app.getGroupId(this@AppDetailsActivity)) {
                    verifyAndInstall(it.downloads)
                }
            }
            else -> {
                purchase()
            }
        }
    }

    val listOfActionsWhenServiceAttaches = ArrayList<Runnable>()

    private fun purchase() {
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        updateActionState(State.PROGRESS)

        if (PathUtil.needsStorageManagerPerm(app.fileList) || this.isExternalStorageEnable()) {
            if (isRAndAbove()) {
                if (!Environment.isExternalStorageManager()) {
                    startForStorageManagerResult.launch(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    )
                } else {
                    updateApp(app)
                }
            } else {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    updateApp(app)
                } else {
                    startForPermissions.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        } else {
            updateApp(app)
        }
    }

    private fun updateApp(app: App) {
        if (updateService == null) {
            listOfActionsWhenServiceAttaches.add {
                updateService?.updateApp(app, true)
            }
            getUpdateServiceInstance()
        } else {
            updateService?.updateApp(app, true)
        }
    }

    private fun updateProgress(
        fetchGroup: FetchGroup,
        etaInMilliSeconds: Long,
        downloadedBytesPerSecond: Long
    ) {
        runOnUiThread {
            val progress = if (fetchGroup.groupDownloadProgress > 0)
                fetchGroup.groupDownloadProgress
            else
                0

            if (progress == 100) {
                B.layoutDetailsInstall.btnDownload.setText(getString(R.string.action_installing))
                return@runOnUiThread
            }
            B.layoutDetailsInstall.apply {
                txtProgressPercent.text = ("${progress}%")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressDownload.setProgress(progress, true)
                } else {
                    progressDownload.progress = progress
                }

                txtEta.text = CommonUtil.getETAString(
                    this@AppDetailsActivity,
                    etaInMilliSeconds
                )
                txtSpeed.text =
                    CommonUtil.getDownloadSpeedString(
                        this@AppDetailsActivity,
                        downloadedBytesPerSecond
                    )
            }
        }
    }

    private fun expandBottomSheet(message: String?) {
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        with(B.layoutDetailsInstall) {
            txtPurchaseError.text = message
            btnDownload.updateState(State.IDLE)
            if (app.isFree)
                btnDownload.setText(R.string.action_install)
            else
                btnDownload.setText(app.price)
        }
    }

    private fun checkAndSetupInstall() {
        isInstalled = PackageUtil.isInstalled(this, app.packageName)

        B.layoutDetailsInstall.btnDownload.let { btn ->
            if (isInstalled) {
                isUpdatable = PackageUtil.isUpdatable(
                    this,
                    app.packageName,
                    app.versionCode.toLong()
                )

                val installedVersion = PackageUtil.getInstalledVersion(this, app.packageName)

                if (isUpdatable) {
                    B.layoutDetailsApp.txtLine3.text =
                        ("$installedVersion ➔ ${app.versionName} (${app.versionCode})")
                    btn.setText(R.string.action_update)
                    btn.addOnClickListener { startDownload() }
                } else {
                    B.layoutDetailsApp.txtLine3.text = installedVersion
                    btn.setText(R.string.action_open)
                    btn.addOnClickListener { openApp() }
                }
                if (!uninstallActionEnabled) {
                    invalidateOptionsMenu()
                }
            } else {
                if (app.isFree) {
                    btn.setText(R.string.action_install)
                } else {
                    btn.setText(app.price)
                }

                btn.addOnClickListener {
                    if (authData.isAnonymous && !app.isFree) {
                        toast(R.string.toast_purchase_blocked)
                    } else {
                        btn.setText(R.string.download_metadata)
                        startDownload()
                    }
                }
                if (uninstallActionEnabled) {
                    invalidateOptionsMenu()
                }
            }
        }
    }

    @Synchronized
    private fun flip(nextView: Int) {
        runOnUiThread {
            val displayChild = B.layoutDetailsInstall.viewFlipper.displayedChild
            if (displayChild != nextView) {
                B.layoutDetailsInstall.viewFlipper.displayedChild = nextView
                if (nextView == 0)
                    checkAndSetupInstall()
            }
        }
    }

    private fun attachFetch() {
        if (fetch == null) {
            downloadManager = DownloadManager.with(this)
            fetch = downloadManager!!.fetch
        }
        fetch?.getFetchGroup(app.getGroupId(this@AppDetailsActivity)) { fetchGroup: FetchGroup ->
            if (fetchGroup.groupDownloadProgress == 100 && fetchGroup.completedDownloads.isNotEmpty()) {
                status = Status.COMPLETED
            } else if (downloadManager?.isDownloading(fetchGroup) == true) {
                status = Status.DOWNLOADING
                flip(1)
            } else if (downloadManager?.isCanceled(fetchGroup) == true) {
                status = Status.CANCELLED
            } else if (fetchGroup.pausedDownloads.isNotEmpty()) {
                status = Status.PAUSED
            } else {
                status = Status.NONE
            }
        }

        fetchGroupListener = object : AbstractFetchGroupListener() {

            override fun onAdded(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.getGroupId(this@AppDetailsActivity)) {
                    status = download.status
                }
            }

            override fun onStarted(
                groupId: Int,
                download: Download,
                downloadBlocks: List<DownloadBlock>,
                totalBlocks: Int,
                fetchGroup: FetchGroup
            ) {
                if (groupId == app.getGroupId(this@AppDetailsActivity)) {
                    status = download.status
                    flip(1)

                    val pkgDir = PathUtil.getPackageDirectory(applicationContext, app.packageName)
                    completionMarker =
                        java.io.File("$pkgDir/.${app.versionCode}.download-complete")
                    inProgressMarker =
                        java.io.File("$pkgDir/.${app.versionCode}.download-in-progress")

                    if (completionMarker.exists())
                        completionMarker.delete()

                    inProgressMarker.createNewFile()
                }
            }

            override fun onResumed(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.getGroupId(this@AppDetailsActivity)) {
                    status = download.status
                    flip(1)
                    inProgressMarker.parentFile?.mkdirs()
                    inProgressMarker.createNewFile()
                }
            }

            override fun onPaused(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.getGroupId(this@AppDetailsActivity)) {
                    status = download.status
                    flip(0)
                }
            }

            override fun onProgress(
                groupId: Int,
                download: Download,
                etaInMilliSeconds: Long,
                downloadedBytesPerSecond: Long,
                fetchGroup: FetchGroup
            ) {
                if (groupId == app.getGroupId(this@AppDetailsActivity)) {
                    updateProgress(fetchGroup, etaInMilliSeconds, downloadedBytesPerSecond)
                    Log.i(
                        "${app.displayName} : ${download.file} -> Progress : %d",
                        fetchGroup.groupDownloadProgress
                    )
                }
            }

            override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.getGroupId(this@AppDetailsActivity) && fetchGroup.groupDownloadProgress == 100) {
                    status = download.status
                    flip(0)
                    updateProgress(fetchGroup, -1, -1)
                    try {
                        inProgressMarker.delete()
                        completionMarker.createNewFile()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }

            override fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.getGroupId(this@AppDetailsActivity)) {
                    status = download.status
                    flip(0)
                    inProgressMarker.delete()
                }
            }

            override fun onError(
                groupId: Int,
                download: Download,
                error: Error,
                throwable: Throwable?,
                fetchGroup: FetchGroup
            ) {
                if (groupId == app.getGroupId(this@AppDetailsActivity)) {
                    status = download.status
                    flip(0)
                    inProgressMarker.delete()
                }
            }
        }

        appMetadataListener = object : AppMetadataStatusListener {
            override fun onAppMetadataStatusError(reason: String, app: App) {
                if (app.packageName == this@AppDetailsActivity.app.packageName) {
                    updateActionState(State.IDLE)
                    expandBottomSheet(reason)
                }
            }
        }

        getUpdateServiceInstance()

        B.layoutDetailsInstall.imgCancel.setOnClickListener {
            fetch?.cancelGroup(
                app.getGroupId(this@AppDetailsActivity)
            )
        }
        if (updateService != null) {
            pendingAddListeners = false
            updateService!!.registerFetchListener(fetchGroupListener)
            // appMetadataListener needs to be initialized after the fetchGroupListener
            updateService!!.registerAppMetadataListener(appMetadataListener)
        } else {
            pendingAddListeners = true
        }
    }

    fun getUpdateServiceInstance() {
        if (updateService == null && !attachToServiceCalled) {
            attachToServiceCalled = true
            val intent = Intent(this, UpdateService::class.java)
            startService(intent)
            bindService(
                intent,
                serviceConnection,
                0
            )
        }
    }

    override fun onPause() {
        if (updateService != null) {
            updateService = null
            attachToServiceCalled = false
            pendingAddListeners = true
            unbindService(serviceConnection)
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (updateService != null) {
            updateService = null
            attachToServiceCalled = false
            pendingAddListeners = true
            unbindService(serviceConnection)
        }
    }

    private fun attachBottomSheet() {
        B.layoutDetailsInstall.apply {
            viewFlipper.setInAnimation(this@AppDetailsActivity, R.anim.fade_in)
            viewFlipper.setOutAnimation(this@AppDetailsActivity, R.anim.fade_out)
        }

        bottomSheetBehavior = BottomSheetBehavior.from(B.layoutDetailsInstall.bottomSheet)
        bottomSheetBehavior.isDraggable = false

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setDraggable(true)
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.isDraggable = false
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }
}
