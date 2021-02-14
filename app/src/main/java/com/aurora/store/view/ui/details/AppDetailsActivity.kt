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
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.File
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.R
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.downloader.RequestBuilder
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.ActivityDetailsBinding
import com.aurora.store.util.*
import com.aurora.store.util.extensions.*
import com.aurora.store.view.ui.downloads.DownloadActivity
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class AppDetailsActivity : BaseDetailsActivity() {

    private lateinit var B: ActivityDetailsBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var app: App
    private lateinit var downloadManager: DownloadManager
    private lateinit var fetch: Fetch
    private lateinit var fetchGroupListener: FetchGroupListener

    private var isNone = false
    private var status = Status.NONE
    private var isInstalled: Boolean = false

    override fun onConnected() {

    }

    override fun onDisconnected() {

    }

    override fun onReconnected() {

    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: BusEvent) {
        when (event) {
            is BusEvent.InstallEvent -> {
                attachActions()
            }
            is BusEvent.UninstallEvent -> {
                attachActions()
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

    override fun onResume() {
        if (!isLAndAbove()) {
            checkAndSetupInstall()
        }
        super.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val itemRaw: String? = intent.getStringExtra(Constants.STRING_EXTRA)
        if (itemRaw != null) {
            app = gson.fromJson(itemRaw, App::class.java)
            isInstalled = PackageUtil.isInstalled(this, app.packageName)

            inflatePartialApp()

            fetchCompleteApp()
        } else {
            close()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_details, menu)
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
            R.id.menu_download_manager -> {
                open(DownloadActivity::class.java)
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

    @Synchronized
    private fun install(files: List<Download>) {
        task {
            AppInstaller.with(this)
                .getPreferredInstaller()
                .install(
                    app.packageName,
                    files
                        .filter { it.file.endsWith(".apk") }
                        .map {
                            it.file
                        }.toList()
                )
        }

        runOnUiThread {
            B.layoutDetailsInstall.btnDownload.setText(getString(R.string.action_installing))
        }
    }

    @Synchronized
    private fun uninstallApp() {
        AppInstaller.with(this)
            .getPreferredInstaller()
            .uninstall(app.packageName)
    }

    private fun attachWhiteListStatus() {

    }

    private fun fetchCompleteApp() {
        task {
            val authData = AuthProvider.with(this).getAuthData()
            return@task AppDetailsHelper.with(authData)
                .using(HttpClient.getPreferredClient())
                .getAppByPackageName(app.packageName)
        } successUi {
            inflateExtraDetails(it)
        } failUi {
            toast("Failed to fetch app details")
        }
    }

    private fun inflatePartialApp() {
        attachWhiteListStatus()
        attachHeader()
        attachToolbar()
        attachBottomSheet()
        attachFetch()
        attachActions()
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
            txtLine3.text = ("v${app.versionName}.${app.versionCode}")

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
        }
    }

    @Synchronized
    private fun startDownload() {
        when (status) {
            Status.PAUSED -> {
                fetch.resumeGroup(app.id)
                isNone = false
            }
            Status.NONE, Status.CANCELLED -> {
                fetch.deleteGroup(app.id)
                isNone = true
            }
            Status.ADDED -> isNone = false
            Status.DOWNLOADING -> {
                isNone = false
                flip(1)
                toast("Already downloading")
            }
            Status.COMPLETED -> {
                fetch.getFetchGroup(app.id) {
                    install(it.downloads)
                }
            }
            else -> {
            }
        }
        if (isNone) {
            purchase()
        }
    }

    private fun purchase() {
        task {
            val authData = AuthProvider
                .with(this)
                .getAuthData()

            PurchaseHelper
                .with(authData)
                .using(HttpClient.getPreferredClient())
                .purchase(app.packageName, app.versionCode, app.offerType)
        } successUi {
            if (it.isNotEmpty()) {
                enqueue(it)
            } else {
                Log.e("Failed to download : ${app.displayName}")
            }
        } failUi {
            expandBottomSheet(it.message)
            Log.e("Failed to purchase ${app.displayName} : ${it.message}")
        }
    }

    private fun enqueue(files: List<File>) = runWithPermissions(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) {
        val requestList = files
            .filter { it.url.isNotEmpty() }
            .map {
                RequestBuilder.buildRequest(this, app, it)
            }
            .toList()

        if (requestList.isNotEmpty()) {
            fetch.enqueue(
                requestList
            ) {
                status = Status.ADDED
                Log.i("Downloading Apks : %s", app.displayName)
            }
        } else {
            expandBottomSheet(getString(R.string.purchase_no_file))
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

        B.layoutDetailsInstall.txtPurchaseError.text = message
        B.layoutDetailsInstall.btnDownload.updateProgress(false)
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

                if (isUpdatable) {
                    btn.setText(getString(R.string.action_update))
                    btn.addOnClickListener {
                        btn.updateProgress(true)
                        startDownload()
                    }
                } else {
                    btn.setText(getString(R.string.action_open))
                    btn.addOnClickListener { openApp() }
                }
            } else {
                if (app.isFree) {
                    btn.setText(getString(R.string.action_install))
                } else {
                    btn.setText(app.price)
                }

                btn.addOnClickListener {
                    btn.setText(getString(R.string.download_metadata))
                    btn.updateProgress(true)
                    startDownload()
                }
            }

            btn.updateProgress(false)
        }
    }

    @Synchronized
    private fun flip(nextView: Int) {
        runOnUiThread {
            B.layoutDetailsInstall.viewFlipper.displayedChild = nextView
            if (nextView == 0)
                checkAndSetupInstall()
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
                    install(fetchGroup.downloads)
                    updateProgress(fetchGroup, -1, -1)
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