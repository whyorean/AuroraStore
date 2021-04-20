/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
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
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import com.aurora.Constants
import com.aurora.extensions.*
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.File
import com.aurora.gplayapi.exceptions.ApiException
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.State
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.downloader.RequestBuilder
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.ActivityDetailsBinding
import com.aurora.store.util.*
import com.aurora.store.view.ui.downloads.DownloadActivity
import com.aurora.store.view.ui.sheets.InstallErrorDialogSheet
import com.aurora.store.view.ui.sheets.ManualDownloadSheet
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class AppDetailsActivity : BaseDetailsActivity() {

    private lateinit var B: ActivityDetailsBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var authData: AuthData
    private lateinit var app: App
    private lateinit var downloadManager: DownloadManager
    private lateinit var fetch: Fetch
    private lateinit var fetchGroupListener: FetchGroupListener

    private var isExternal = false
    private var isNone = false
    private var status = Status.NONE
    private var isInstalled: Boolean = false
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
    }

    override fun onBackPressed() {
        if (isExternal) {
            open(MainActivity::class.java, true)
        }
        super.onBackPressed()

    }

    override fun onResume() {
        if (!isLAndAbove()) {
            checkAndSetupInstall()
        }
        super.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.scheme != null && (intent.scheme == "market" || intent.scheme == "http" || intent.scheme == "https")) {
            val packageName = intent.data!!.getQueryParameter("id")
            val packageVersion = intent.data!!.getQueryParameter("v")
            if (packageName.isNullOrEmpty()) {
                close()
            } else {
                isExternal = true
                app = App(packageName)
                if (!packageVersion.isNullOrEmpty()) {
                    app.versionCode = packageVersion.toInt()
                }
                fetchCompleteApp()
            }

            autoDownload = intent.data!!.getBooleanQueryParameter("download", false)
            downloadOnly = !intent.data!!.getBooleanQueryParameter("install", false)
        } else {
            val rawApp: String? = intent.getStringExtra(Constants.STRING_EXTRA)
            if (rawApp != null) {
                app = gson.fromJson(rawApp, App::class.java)
                isInstalled = PackageUtil.isInstalled(this, app.packageName)

                inflatePartialApp()
                fetchCompleteApp()
            } else {
                close()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_details, menu)
        if (::app.isInitialized) {
            val installed = PackageUtil.isInstalled(this, app.packageName)
            menu?.findItem(R.id.action_uninstall)?.isVisible = installed
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
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
            R.id.menu_download_manager -> {
                open(DownloadActivity::class.java)
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
            filesExist = filesExist && FileUtils.getFile(download.file).exists()
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
                AppInstaller(this)
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
            AppInstaller(this)
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
                fetch.resumeGroup(app.id)
            }
            Status.DOWNLOADING -> {
                flip(1)
                toast("Already downloading")
            }
            Status.COMPLETED -> {
                fetch.getFetchGroup(app.id) {
                    verifyAndInstall(it.downloads)
                }
            }
            else -> {
                purchase()
            }
        }
    }

    private fun purchase() {
        updateActionState(State.PROGRESS)

        task {
            val authData = AuthProvider
                .with(this)
                .getAuthData()

            PurchaseHelper(authData)
                .using(HttpClient.getPreferredClient())
                .purchase(app.packageName, app.versionCode, app.offerType)
        } successUi { files ->
            if (files.isNotEmpty()) {
                var hasOBB = false

                files.forEach { file ->
                    if (file.type == File.FileType.OBB || file.type == File.FileType.PATCH) {
                        hasOBB = true
                    }
                }

                if (hasOBB)
                    enqueueWithStoragePermission(files)
                else
                    enqueue(files)
            } else {
                Log.e("Failed to download : ${app.displayName}")
                updateActionState(State.IDLE)
            }
        } failUi {
            updateActionState(State.IDLE)
            var reason = "Unknown"

            when (it) {
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

            expandBottomSheet(reason)

            Log.e("Failed to purchase ${app.displayName} : $reason")
        }
    }

    private fun enqueueWithStoragePermission(files: List<File>) = runWithPermissions(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) {
        enqueue(files)
    }

    private fun enqueue(files: List<File>) {
        val requestList = files
            .filter { it.url.isNotEmpty() }
            .map {
                RequestBuilder.buildRequest(this, app, it)
            }
            .toList()

        if (requestList.isNotEmpty()) {
            /*Remove old fetch group if downloaded earlier, mostly in case of updates*/
            fetch.deleteGroup(app.id)

            /*Enqueue new fetch group*/
            fetch.enqueue(
                requestList
            ) {
                status = Status.ADDED
                Log.i("Downloading Apks : %s", app.displayName)
            }
        } else {
            updateActionState(State.IDLE)
            expandBottomSheet(getString(R.string.purchase_session_expired))
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
                val isUpdatable = PackageUtil.isUpdatable(
                    this,
                    app.packageName,
                    app.versionCode.toLong()
                )

                val installedVersion = PackageUtil.getInstalledVersion(this, app.packageName)

                if (isUpdatable) {
                    B.layoutDetailsApp.txtLine3.text =
                        ("$installedVersion > ${app.versionName} (${app.versionCode})")
                    btn.setText(R.string.action_update)
                    btn.addOnClickListener { startDownload() }
                } else {
                    B.layoutDetailsApp.txtLine3.text = installedVersion
                    btn.setText(R.string.action_open)
                    btn.addOnClickListener { openApp() }
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
        downloadManager = DownloadManager.with(this)
        fetch = downloadManager.fetch

        fetch.getFetchGroup(app.id) { fetchGroup: FetchGroup ->
            if (fetchGroup.groupDownloadProgress == 100 && fetchGroup.completedDownloads.isNotEmpty()) {
                status = Status.COMPLETED
            } else if (downloadManager.isDownloading(fetchGroup)) {
                status = Status.DOWNLOADING
                flip(1)
            } else if (downloadManager.isCanceled(fetchGroup)) {
                status = Status.CANCELLED
            } else if (fetchGroup.pausedDownloads.isNotEmpty()) {
                status = Status.PAUSED
            } else {
                status = Status.NONE
            }
        }

        fetchGroupListener = object : AbstractFetchGroupListener() {

            override fun onStarted(
                groupId: Int,
                download: Download,
                downloadBlocks: List<DownloadBlock>,
                totalBlocks: Int,
                fetchGroup: FetchGroup
            ) {
                if (groupId == app.id) {
                    status = download.status
                    flip(1)
                }
            }

            override fun onResumed(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.id) {
                    status = download.status
                    flip(1)
                }
            }

            override fun onPaused(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.id) {
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
                if (groupId == app.id) {
                    updateProgress(fetchGroup, etaInMilliSeconds, downloadedBytesPerSecond)
                    Log.i(
                        "${app.displayName} : ${download.file} -> Progress : %d",
                        fetchGroup.groupDownloadProgress
                    )
                }
            }

            override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.id && fetchGroup.groupDownloadProgress == 100) {
                    status = download.status
                    flip(0)
                    updateProgress(fetchGroup, -1, -1)
                    try {
                        verifyAndInstall(fetchGroup.downloads)
                    } catch (e: Exception) {
                        Log.e(e.stackTraceToString())
                    }
                }
            }

            override fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.id) {
                    status = download.status
                    flip(0)
                }
            }

            override fun onError(
                groupId: Int,
                download: Download,
                error: Error,
                throwable: Throwable?,
                fetchGroup: FetchGroup
            ) {
                if (groupId == app.id) {
                    status = download.status
                    flip(0)
                }
            }
        }

        fetch.addListener(fetchGroupListener)

        B.layoutDetailsInstall.imgCancel.setOnClickListener {
            fetch.cancelGroup(
                app.id
            )
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