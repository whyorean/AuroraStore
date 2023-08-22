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
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import coil.transform.RoundedCornersTransformation
import com.airbnb.epoxy.EpoxyRecyclerView
import com.aurora.Constants
import com.aurora.extensions.browse
import com.aurora.extensions.getString
import com.aurora.extensions.hide
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.runOnUiThread
import com.aurora.extensions.share
import com.aurora.extensions.show
import com.aurora.extensions.showDialog
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.R
import com.aurora.store.State
import com.aurora.store.data.ViewState
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.downloader.getGroupId
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.service.AppMetadataStatusListener
import com.aurora.store.data.service.UpdateService
import com.aurora.store.databinding.FragmentDetailsBinding
import com.aurora.store.databinding.LayoutDetailsBetaBinding
import com.aurora.store.databinding.LayoutDetailsDescriptionBinding
import com.aurora.store.databinding.LayoutDetailsDevBinding
import com.aurora.store.databinding.LayoutDetailsPermissionsBinding
import com.aurora.store.databinding.LayoutDetailsReviewBinding
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.Log
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.isExternalStorageEnable
import com.aurora.store.view.custom.RatingView
import com.aurora.store.view.epoxy.controller.DetailsCarouselController
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.view.epoxy.views.details.ReviewViewModel_
import com.aurora.store.view.epoxy.views.details.ScreenshotView
import com.aurora.store.view.epoxy.views.details.ScreenshotViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.details.DetailsClusterViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.tonyodev.fetch2.AbstractFetchGroupListener
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchGroup
import com.tonyodev.fetch2.FetchGroupListener
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.DownloadBlock
import java.io.File
import java.util.Locale
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AppDetailsFragment : BaseFragment(R.layout.fragment_details) {

    private var _binding: FragmentDetailsBinding? = null
    private val binding: FragmentDetailsBinding
        get() = _binding!!

    private val viewModel: AppDetailsViewModel by viewModels()

    private val args: AppDetailsFragmentArgs by navArgs()

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
    private lateinit var completionMarker: File
    private lateinit var inProgressMarker: File

    private var isExternal = false
    private var isNone = false
    private var status = Status.NONE
    private var isInstalled: Boolean = false
    private var isUpdatable: Boolean = false
    private var autoDownload: Boolean = false
    private var downloadOnly: Boolean = false
    private var uninstallActionEnabled = false

    val listOfActionsWhenServiceAttaches = ArrayList<Runnable>()

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
                    binding.layoutDetailsToolbar.toolbar.menu.apply {
                        findItem(R.id.action_uninstall)?.isVisible = true
                    }
                }
            }

            is BusEvent.UninstallEvent -> {
                if (app.packageName == event.packageName) {
                    attachActions()
                    binding.layoutDetailsToolbar.toolbar.menu.apply {
                        findItem(R.id.action_uninstall)?.isVisible = false
                    }
                }
            }

            is BusEvent.ManualDownload -> {
                if (app.packageName == event.packageName) {
                    app.versionCode = event.versionCode
                    purchase()
                }
            }

            is InstallerEvent.Failed -> {
                findNavController().navigate(
                    AppDetailsFragmentDirections.actionAppDetailsFragmentToInstallErrorDialogSheet(
                        app,
                        event.packageName ?: "",
                        event.error ?: "",
                        event.extra ?: ""
                    )
                )
            }

            else -> {

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailsBinding.bind(view)

        // TODO: Move to viewModel
        authData = AuthProvider.with(view.context).getAuthData()

        if (args.app != null) {
            app = args.app!!
            isInstalled = PackageUtil.isInstalled(requireContext(), app.packageName)

            inflatePartialApp()
        } else {
            isExternal = true
            app = App(args.packageName)
        }

        // App Details
        viewModel.fetchAppDetails(view.context, app.packageName)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.app.collect {
                if (app.packageName.isNotBlank()) {
                    if (isExternal) {
                        app = it
                        inflatePartialApp()
                    }
                    inflateExtraDetails(it)
                    viewModel.fetchAppReviews(view.context, app.packageName)
                } else {
                    toast("Failed to fetch app details")
                }
            }
        }

        // Reviews
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reviews.collect {
                binding.layoutDetailsReview.epoxyRecycler.withModels {
                    it.take(4).forEach { add(ReviewViewModel_().id(it.timeStamp).review(it)) }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userReview.collect {
                if (it.timeStamp == 0L) {
                    binding.layoutDetailsReview.userStars.rating = it.rating.toFloat()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_rated_success),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_rated_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Report (Exodus Privacy)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.report.collect { report ->
                if (report == null) {
                    binding.layoutDetailsPrivacy.txtStatus.text =
                        getString(R.string.failed_to_fetch_report)
                    return@collect
                }

                if (report.trackers.isNotEmpty()) {
                    binding.layoutDetailsPrivacy.txtStatus.apply {
                        setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                if (report.trackers.size > 4)
                                    R.color.colorRed
                                else
                                    R.color.colorOrange
                            )
                        )
                        text = "${report.trackers.size} ${getString(R.string.exodus_substring)} ${report.version}"
                    }

                    binding.layoutDetailsPrivacy.headerPrivacy.addClickListener {
                        findNavController().navigate(AppDetailsFragmentDirections
                                .actionAppDetailsFragmentToDetailsExodusFragment(report)
                        )
                    }
                } else {
                    binding.layoutDetailsPrivacy.txtStatus.apply {
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.colorGreen))
                        text = getString(R.string.exodus_no_tracker)
                    }
                }
            }
        }

        // Beta program
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.testingProgramStatus.collect {
                if (it != null) {
                    binding.layoutDetailsBeta.btnBetaAction.isEnabled = true
                    if (it.subscribed) {
                        updateBetaActions(binding.layoutDetailsBeta, true)
                    }
                    if (it.unsubscribed) {
                        updateBetaActions(binding.layoutDetailsBeta, false)
                    }
                } else {
                    app.testingProgram?.let { testingProgram ->
                        updateBetaActions(binding.layoutDetailsBeta, testingProgram.isSubscribed)
                        toast(getString(R.string.details_beta_delay))
                    }
                }
            }
        }

        // Toolbar
        binding.layoutDetailsToolbar.toolbar.apply {
            elevation = 0f
            navigationIcon = ContextCompat.getDrawable(view.context, R.drawable.ic_arrow_back)
            setNavigationOnClickListener {
                if (isExternal) {
                    activity?.finish()
                } else {
                    findNavController().navigateUp()
                }
            }
            inflateMenu(R.menu.menu_details)

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_share -> {
                        view.context.share(app)
                    }

                    R.id.action_uninstall -> {
                        uninstallApp()
                    }

                    R.id.menu_download_manual -> {
                        findNavController().navigate(
                            AppDetailsFragmentDirections
                                .actionAppDetailsFragmentToManualDownloadSheet(app)
                        )
                    }

                    R.id.menu_download_manager -> {
                        findNavController().navigate(R.id.downloadFragment)
                    }

                    R.id.action_playstore -> {
                        view.context.browse("${Constants.SHARE_URL}${app.packageName}")
                    }
                }
                true
            }

            if (::app.isInitialized) {
                val installed = PackageUtil.isInstalled(requireContext(), app.packageName)
                menu?.findItem(R.id.action_uninstall)?.isVisible = installed
                uninstallActionEnabled = installed
            }
        }
    }

    override fun onResume() {
        getUpdateServiceInstance()
        checkAndSetupInstall()
        super.onResume()
    }

    private fun attachActions() {
        flip(0)
        checkAndSetupInstall()
    }

    private fun updateActionState(state: State) {
        binding.layoutDetailsInstall.btnDownload.updateState(state)
    }

    private fun openApp() {
        val intent = PackageUtil.getLaunchIntent(requireContext(), app.packageName)
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
        val preferredInstaller =
            Preferences.getInteger(requireContext(), Preferences.PREFERENCE_INSTALLER_ID)

        if (apkFiles.size > 1 && preferredInstaller == 1) {
            showDialog(R.string.title_installer, R.string.dialog_desc_native_split)
        } else {
            viewModel.install(requireContext(), app.packageName, apkFiles.map { it.file })

            runOnUiThread {
                binding.layoutDetailsInstall.btnDownload.setText(getString(R.string.action_installing))
            }
        }
    }

    @Synchronized
    private fun uninstallApp() {
        AppInstaller.getInstance(requireContext()).getPreferredInstaller()
            .uninstall(app.packageName)
    }

    private fun attachWhiteListStatus() {

    }

    private fun inflatePartialApp() {
        if (::app.isInitialized) {
            attachWhiteListStatus()
            attachHeader()
            attachBottomSheet()
            attachFetch()
            attachActions()

            if (autoDownload) {
                purchase()
            }
        }
    }

    private fun attachHeader() {
        binding.layoutDetailsApp.apply {
            imgIcon.load(app.iconArtwork.url) {
                placeholder(R.drawable.bg_placeholder)
                transformations(RoundedCornersTransformation(32F))
            }

            txtLine1.text = app.displayName
            txtLine2.text = app.developerName
            txtLine2.setOnClickListener {
                findNavController().navigate(
                    AppDetailsFragmentDirections
                        .actionAppDetailsFragmentToDevAppsFragment(app.developerName)
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
            binding.viewFlipper.displayedChild = 1
            inflateAppDescription(binding.layoutDetailDescription, app)
            inflateAppRatingAndReviews(binding.layoutDetailsReview, app)
            inflateAppDevInfo(binding.layoutDetailsDev, app)
            inflateAppPrivacy(app)
            inflateAppPermission(binding.layoutDetailsPermissions, app)

            if (!authData.isAnonymous) {
                app.testingProgram?.let {
                    if (it.isAvailable && it.isSubscribed) {
                        binding.layoutDetailsApp.txtLine1.text = it.displayName
                    }
                }

                inflateBetaSubscription(binding.layoutDetailsBeta, app)
            }

            if (Preferences.getBoolean(requireContext(), Preferences.PREFERENCE_SIMILAR)) {
                inflateAppStream(binding.epoxyRecyclerStream, app)
            }
        }
    }

    @Synchronized
    private fun startDownload() {
        when (status) {
            Status.PAUSED -> {
                fetch?.resumeGroup(app.getGroupId(requireContext()))
            }

            Status.DOWNLOADING -> {
                flip(1)
                toast("Already downloading")
            }

            Status.COMPLETED -> {
                fetch?.getFetchGroup(app.getGroupId(requireContext())) {
                    verifyAndInstall(it.downloads)
                }
            }

            else -> {
                purchase()
            }
        }
    }

    private fun purchase() {
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        updateActionState(State.PROGRESS)

        if (PathUtil.needsStorageManagerPerm(app.fileList) || requireContext().isExternalStorageEnable()) {
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
                        requireContext(),
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
                binding.layoutDetailsInstall.btnDownload.setText(getString(R.string.action_installing))
                return@runOnUiThread
            }
            binding.layoutDetailsInstall.apply {
                txtProgressPercent.text = ("${progress}%")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressDownload.setProgress(progress, true)
                } else {
                    progressDownload.progress = progress
                }

                txtEta.text = CommonUtil.getETAString(
                    requireContext(),
                    etaInMilliSeconds
                )
                txtSpeed.text =
                    CommonUtil.getDownloadSpeedString(
                        requireContext(),
                        downloadedBytesPerSecond
                    )
            }
        }
    }

    private fun expandBottomSheet(message: String?) {
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        with(binding.layoutDetailsInstall) {
            txtPurchaseError.text = message
            btnDownload.updateState(State.IDLE)
            if (app.isFree)
                btnDownload.setText(R.string.action_install)
            else
                btnDownload.setText(app.price)
        }
    }

    private fun checkAndSetupInstall() {
        isInstalled = PackageUtil.isInstalled(requireContext(), app.packageName)

        binding.layoutDetailsInstall.btnDownload.let { btn ->
            if (isInstalled) {
                isUpdatable = PackageUtil.isUpdatable(
                    requireContext(),
                    app.packageName,
                    app.versionCode.toLong()
                )

                val installedVersion =
                    PackageUtil.getInstalledVersion(requireContext(), app.packageName)

                if (isUpdatable) {
                    binding.layoutDetailsApp.txtLine3.text =
                        ("$installedVersion ➔ ${app.versionName} (${app.versionCode})")
                    btn.setText(R.string.action_update)
                    btn.addOnClickListener { startDownload() }
                } else {
                    binding.layoutDetailsApp.txtLine3.text = installedVersion
                    btn.setText(R.string.action_open)
                    btn.addOnClickListener { openApp() }
                }
                if (!uninstallActionEnabled) {
                    binding.layoutDetailsToolbar.toolbar.invalidateMenu()
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
                    binding.layoutDetailsToolbar.toolbar.invalidateMenu()
                }
            }
        }
    }

    @Synchronized
    private fun flip(nextView: Int) {
        runOnUiThread {
            val displayChild = binding.layoutDetailsInstall.viewFlipper.displayedChild
            if (displayChild != nextView) {
                binding.layoutDetailsInstall.viewFlipper.displayedChild = nextView
                if (nextView == 0) checkAndSetupInstall()
            }
        }
    }

    private fun attachFetch() {
        if (fetch == null) {
            downloadManager = DownloadManager.with(requireContext())
            fetch = downloadManager!!.fetch
        }
        fetch?.getFetchGroup(app.getGroupId(requireContext())) { fetchGroup: FetchGroup ->
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
                if (groupId == app.getGroupId(requireContext())) {
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
                if (groupId == app.getGroupId(requireContext())) {
                    status = download.status
                    flip(1)

                    val pkgDir = PathUtil.getPackageDirectory(requireContext(), app.packageName)
                    completionMarker =
                        File("$pkgDir/.${app.versionCode}.download-complete")
                    inProgressMarker =
                        File("$pkgDir/.${app.versionCode}.download-in-progress")

                    if (completionMarker.exists())
                        completionMarker.delete()

                    inProgressMarker.createNewFile()
                }
            }

            override fun onResumed(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.getGroupId(requireContext())) {
                    status = download.status
                    flip(1)
                    inProgressMarker.parentFile?.mkdirs()
                    inProgressMarker.createNewFile()
                }
            }

            override fun onPaused(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.getGroupId(requireContext())) {
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
                if (groupId == app.getGroupId(requireContext())) {
                    updateProgress(fetchGroup, etaInMilliSeconds, downloadedBytesPerSecond)
                    Log.i(
                        "${app.displayName} : ${download.file} -> Progress : %d",
                        fetchGroup.groupDownloadProgress
                    )
                }
            }

            override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (groupId == app.getGroupId(requireContext()) && fetchGroup.groupDownloadProgress == 100) {
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
                if (groupId == app.getGroupId(requireContext())) {
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
                if (groupId == app.getGroupId(requireContext())) {
                    status = download.status
                    flip(0)
                    inProgressMarker.delete()
                }
            }
        }

        appMetadataListener = object : AppMetadataStatusListener {
            override fun onAppMetadataStatusError(reason: String, app: App) {
                if (app.packageName == this@AppDetailsFragment.app.packageName) {
                    updateActionState(State.IDLE)
                    expandBottomSheet(reason)
                }
            }
        }

        getUpdateServiceInstance()

        binding.layoutDetailsInstall.imgCancel.setOnClickListener {
            fetch?.cancelGroup(
                app.getGroupId(requireContext())
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

    private fun getUpdateServiceInstance() {
        if (updateService == null && !attachToServiceCalled) {
            attachToServiceCalled = true
            val intent = Intent(requireContext(), UpdateService::class.java)
            activity?.startService(intent)
            activity?.bindService(
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
            activity?.unbindService(serviceConnection)
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (updateService != null) {
            updateService = null
            attachToServiceCalled = false
            pendingAddListeners = true
            activity?.unbindService(serviceConnection)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun attachBottomSheet() {
        binding.layoutDetailsInstall.apply {
            viewFlipper.setInAnimation(requireContext(), R.anim.fade_in)
            viewFlipper.setOutAnimation(requireContext(), R.anim.fade_out)
        }

        bottomSheetBehavior = BottomSheetBehavior.from(binding.layoutDetailsInstall.bottomSheet)
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

    //Sub Section Inflation
    private fun inflateAppDescription(B: LayoutDetailsDescriptionBinding, app: App) {
        val installs = CommonUtil.addDiPrefix(app.installs)

        if (installs != "NA") {
            B.txtInstalls.text = CommonUtil.addDiPrefix(app.installs)
        } else {
            B.txtInstalls.hide()
        }

        B.txtSize.text = CommonUtil.addSiPrefix(app.size)
        B.txtRating.text = app.labeledRating
        B.txtSdk.text = ("Target SDK ${app.targetSdk}")
        B.txtUpdated.text = app.updatedOn
        B.txtDescription.text = HtmlCompat.fromHtml(
            app.shortDescription,
            HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS
        )

        app.changes.apply {
            if (isEmpty()) {
                B.txtChangelog.text = getString(R.string.details_changelog_unavailable)
            } else {
                B.txtChangelog.text = HtmlCompat.fromHtml(
                    this,
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            }
        }

        B.headerDescription.addClickListener {
            findNavController().navigate(
                AppDetailsFragmentDirections.actionAppDetailsFragmentToDetailsMoreFragment(app)
            )
        }

        B.epoxyRecycler.withModels {
            setFilterDuplicates(true)
            var position = 0
            app.screenshots
                //.sortedWith { o1, o2 -> o2.height.compareTo(o1.height) }
                .forEach { artwork ->
                    add(
                        ScreenshotViewModel_()
                            .id(artwork.url)
                            .artwork(artwork)
                            .position(position++)
                            .callback(object : ScreenshotView.ScreenshotCallback {
                                override fun onClick(position: Int) {
                                    openScreenshotFragment(app, position)
                                }
                            })
                    )
                }
        }
    }

    private fun inflateAppRatingAndReviews(B: LayoutDetailsReviewBinding, app: App) {
        B.averageRating.text = app.rating.average.toString()
        B.txtReviewCount.text = app.rating.abbreviatedLabel

        var totalStars = 0L
        totalStars += app.rating.oneStar
        totalStars += app.rating.twoStar
        totalStars += app.rating.threeStar
        totalStars += app.rating.fourStar
        totalStars += app.rating.fiveStar

        B.avgRatingLayout.apply {
            removeAllViews()
            addView(addAvgReviews(5, totalStars, app.rating.fiveStar))
            addView(addAvgReviews(4, totalStars, app.rating.fourStar))
            addView(addAvgReviews(3, totalStars, app.rating.threeStar))
            addView(addAvgReviews(2, totalStars, app.rating.twoStar))
            addView(addAvgReviews(1, totalStars, app.rating.oneStar))
        }

        B.averageRating.text = String.format(Locale.getDefault(), "%.1f", app.rating.average)
        B.txtReviewCount.text = app.rating.abbreviatedLabel

        val authData = AuthProvider.with(requireContext()).getAuthData()

        B.layoutUserReview.visibility = if (authData.isAnonymous) View.GONE else View.VISIBLE

        B.btnPostReview.setOnClickListener {
            if (authData.isAnonymous) {
                toast(R.string.toast_anonymous_restriction)
            } else {
                addOrUpdateReview(app, Review().apply {
                    title = authData.userProfile!!.name
                    rating = B.userStars.rating.toInt()
                    comment = B.inputReview.text.toString()
                })
            }
        }

        B.headerRatingReviews.addClickListener {
            findNavController().navigate(
                AppDetailsFragmentDirections.actionAppDetailsFragmentToDetailsReviewFragment(
                    app.displayName,
                    app.packageName
                )
            )
        }
    }

    private fun inflateAppPrivacy(app: App) {
        viewModel.fetchAppReport(app.packageName)
    }

    private fun inflateAppDevInfo(B: LayoutDetailsDevBinding, app: App) {
        if (app.developerAddress.isNotEmpty()) {
            B.devAddress.apply {
                setTxtSubtitle(
                    HtmlCompat.fromHtml(
                        app.developerAddress,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ).toString()
                )
                visibility = View.VISIBLE
            }
        }

        if (app.developerWebsite.isNotEmpty()) {
            B.devWeb.apply {
                setTxtSubtitle(app.developerWebsite)
                visibility = View.VISIBLE
            }
        }

        if (app.developerEmail.isNotEmpty()) {
            B.devMail.apply {
                setTxtSubtitle(app.developerEmail)
                visibility = View.VISIBLE
            }
        }
    }

    private fun inflateBetaSubscription(B: LayoutDetailsBetaBinding, app: App) {
        app.testingProgram?.let { betaProgram ->
            if (betaProgram.isAvailable) {
                B.root.show()

                updateBetaActions(B, betaProgram.isSubscribed)

                if (betaProgram.isSubscribedAndInstalled) {

                }

                B.imgBeta.load(betaProgram.artwork.url) {

                }

                B.btnBetaAction.setOnClickListener {
                    B.btnBetaAction.text = getString(R.string.action_pending)
                    B.btnBetaAction.isEnabled = false
                    viewModel.fetchTestingProgramStatus(
                        requireContext(),
                        app.packageName,
                        !betaProgram.isSubscribed
                    )
                }
            } else {
                B.root.hide()
            }
        }
    }

    private fun inflateAppStream(epoxyRecyclerView: EpoxyRecyclerView, app: App) {
        app.detailsStreamUrl?.let {
            val VM = ViewModelProvider(this)[DetailsClusterViewModel::class.java]

            val carouselController =
                DetailsCarouselController(object : GenericCarouselController.Callbacks {
                    override fun onHeaderClicked(streamCluster: StreamCluster) {
                        if (streamCluster.clusterBrowseUrl.isNotEmpty())
                            openStreamBrowseFragment(
                                streamCluster.clusterBrowseUrl,
                                streamCluster.clusterTitle
                            )
                        else
                            toast(getString(R.string.toast_page_unavailable))
                    }

                    override fun onClusterScrolled(streamCluster: StreamCluster) {
                        VM.observeCluster(streamCluster)
                    }

                    override fun onAppClick(app: App) {
                        openDetailsFragment(app)
                    }

                    override fun onAppLongClick(app: App) {

                    }
                })

            VM.liveData.observe(viewLifecycleOwner) {
                when (it) {
                    is ViewState.Empty -> {
                    }

                    is ViewState.Loading -> {

                    }

                    is ViewState.Error -> {

                    }

                    is ViewState.Status -> {

                    }

                    is ViewState.Success<*> -> {
                        carouselController.setData(it.data as StreamBundle)
                    }
                }
            }

            epoxyRecyclerView.setController(carouselController)

            VM.getStreamBundle(it)
        }
    }

    private fun inflateAppPermission(B: LayoutDetailsPermissionsBinding, app: App) {
        B.headerPermission.addClickListener {
            if (app.permissions.size > 0) {
                findNavController().navigate(
                    AppDetailsFragmentDirections.actionAppDetailsFragmentToPermissionBottomSheet(
                        app
                    )
                )
            }
        }
        B.txtPermissionCount.text = ("${app.permissions.size} permissions")
    }

    private fun updateBetaActions(B: LayoutDetailsBetaBinding, isSubscribed: Boolean) {
        if (isSubscribed) {
            B.btnBetaAction.text = getString(R.string.action_leave)
            B.txtBetaTitle.text = getString(R.string.details_beta_subscribed)
        } else {
            B.btnBetaAction.text = getString(R.string.action_join)
            B.txtBetaTitle.text = getString(R.string.details_beta_available)
        }
    }

    /* App Review Helpers */

    private fun addAvgReviews(number: Int, max: Long, rating: Long): RelativeLayout {
        return RatingView(requireContext(), number, max.toInt(), rating.toInt())
    }

    private fun addOrUpdateReview(app: App, review: Review, isBeta: Boolean = false) {
        viewModel.postAppReview(requireContext(), app.packageName, review, isBeta)
    }
}
