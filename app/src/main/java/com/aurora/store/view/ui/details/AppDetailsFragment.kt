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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import coil.transform.RoundedCornersTransformation
import com.aurora.Constants
import com.aurora.Constants.EXODUS_SUBMIT_PAGE
import com.aurora.extensions.accentColor
import com.aurora.extensions.browse
import com.aurora.extensions.contrastingColor
import com.aurora.extensions.getString
import com.aurora.extensions.hide
import com.aurora.extensions.runOnUiThread
import com.aurora.extensions.share
import com.aurora.extensions.show
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.File
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.data.models.datasafety.EntryType
import com.aurora.store.AppStreamStash
import com.aurora.store.AuroraApp
import com.aurora.store.PermissionType
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.event.Event
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.State
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.model.ViewState.Loading.getDataAs
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.PermissionProvider
import com.aurora.store.databinding.FragmentDetailsBinding
import com.aurora.store.util.CertUtil
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.ShortcutManagerUtil
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import com.aurora.gplayapi.data.models.datasafety.Report as DataSafetyReport

@AndroidEntryPoint
class AppDetailsFragment : BaseFragment<FragmentDetailsBinding>() {

    private val viewModel: AppDetailsViewModel by activityViewModels()
    private val detailsClusterViewModel: DetailsClusterViewModel by activityViewModels()

    private val args: AppDetailsFragmentArgs by navArgs()

    @Inject
    lateinit var authProvider: AuthProvider


    private lateinit var permissionProvider: PermissionProvider
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var app: App

    private var streamBundle: StreamBundle? = StreamBundle()

    private var isExternal = false
    private var downloadStatus = DownloadStatus.UNAVAILABLE
    private var isUpdatable: Boolean = false
    private var autoDownload: Boolean = false
    private var uninstallActionEnabled = false

    private fun onEvent(event: Event) {
        when (event) {
            is InstallerEvent.Installed -> {
                if (app.packageName == event.packageName) {
                    attachActions()
                    binding.layoutDetailsToolbar.toolbar.menu.apply {
                        findItem(R.id.action_home_screen)?.isVisible =
                            ShortcutManagerUtil.canPinShortcut(requireContext(), app.packageName)
                        findItem(R.id.action_uninstall)?.isVisible = true
                        findItem(R.id.menu_app_settings)?.isVisible = true
                    }
                }
            }

            is InstallerEvent.Uninstalled -> {
                if (app.packageName == event.packageName) {
                    attachActions()
                    binding.layoutDetailsToolbar.toolbar.menu.apply {
                        findItem(R.id.action_home_screen)?.isVisible = false
                        findItem(R.id.action_uninstall)?.isVisible = false
                        findItem(R.id.menu_app_settings)?.isVisible = false
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
                if (app.packageName == event.packageName) {
                    findNavController().navigate(
                        AppDetailsFragmentDirections.actionAppDetailsFragmentToInstallErrorDialogSheet(
                            app,
                            event.packageName,
                            event.error,
                            event.extra
                        )
                    )
                }
            }

            is InstallerEvent.Installing -> {
                if (event.packageName == app.packageName) {
                    attachActions()
                    updateActionState(State.INSTALLING)
                }
            }

            else -> {

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionProvider = PermissionProvider(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.app != null) {
            app = args.app!!
            inflatePartialApp()
        } else {
            isExternal = true
            app = App(args.packageName)
        }

        // Toolbar
        attachToolbar()

        // Check whether app is installed or not
        app.isInstalled = PackageUtil.isInstalled(requireContext(), app.packageName)

        // App Details
        viewModel.fetchAppDetails(app.packageName)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.app.collect {
                if (it.packageName.isNotBlank()) {
                    app = it
                    inflatePartialApp() // Re-inflate the app details, as web data may vary.
                    inflateExtraDetails(app)
                    viewModel.fetchAppReviews(app.packageName)
                } else {
                    toast("Failed to fetch app details")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.downloadsList
                .filter { list -> list.any { it.packageName == app.packageName } }
                .collectLatest { downloadsList ->
                    val download = downloadsList.find { it.packageName == app.packageName }
                    download?.let {
                        downloadStatus = it.downloadStatus

                        if (it.isFinished) flip(0) else flip(1)
                        when (it.downloadStatus) {
                            DownloadStatus.QUEUED -> {
                                updateProgress(it.progress)
                            }

                            DownloadStatus.DOWNLOADING -> {
                                updateProgress(it.progress, it.speed, it.timeRemaining)
                            }

                            else -> {}
                        }
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

        // User Rating
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userReview.collect {
                if (it.commentId.isNotEmpty()) {
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

        // Data Safety Report
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dataSafetyReport.collect { updateDataSafetyViews(it) }
        }

        // Exodus Privacy Report
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.exodusReport.collect { report ->
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
                        text =
                            "${report.trackers.size} ${getString(R.string.exodus_substring)} ${report.version}"
                    }

                    binding.layoutDetailsPrivacy.headerPrivacy.addClickListener {
                        findNavController().navigate(
                            AppDetailsFragmentDirections
                                .actionAppDetailsFragmentToDetailsExodusFragment(
                                    app.displayName,
                                    report
                                )
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
                    binding.layoutDetailsBeta.btnBetaAction.apply {
                        isEnabled = true
                        setTextColor(contrastingColor(requireContext().accentColor()))
                    }
                    if (it.subscribed) {
                        updateBetaActions(true)
                    }
                    if (it.unsubscribed) {
                        updateBetaActions(false)
                    }
                } else {
                    app.testingProgram?.let { testingProgram ->
                        updateBetaActions(testingProgram.isSubscribed)
                        toast(getString(R.string.details_beta_delay))
                    }
                }
            }
        }

        // Favorites
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favourite.collect {
                if (it) {
                    binding.layoutDetailsToolbar.toolbar.menu
                        ?.findItem(R.id.action_favourite)
                        ?.setIcon(R.drawable.ic_favorite_checked)
                } else {
                    binding.layoutDetailsToolbar.toolbar.menu
                        ?.findItem(R.id.action_favourite)
                        ?.setIcon(R.drawable.ic_favorite_unchecked)
                }
            }
        }

        // Misc Bindings
        binding.layoutDetailsPrivacy.btnRequestAnalysis.apply {
            setOnClickListener {
                it.context.browse("${EXODUS_SUBMIT_PAGE}${app.packageName}")
            }
            setTextColor(contrastingColor(requireContext().accentColor()))
        }
        binding.layoutDetailsInstall.progressDownload.clipToOutline = true
        binding.layoutDetailsInstall.imgCancel.setOnClickListener {
            viewModel.cancelDownload(app)
            if (downloadStatus != DownloadStatus.DOWNLOADING) flip(0)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            AuroraApp.events.busEvent.collect { onEvent(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            AuroraApp.events.installerEvent.collect { onEvent(it) }
        }
    }

    override fun onResume() {
        checkAndSetupInstall()
        super.onResume()
    }

    override fun onDestroy() {
        permissionProvider.unregister()
        super.onDestroy()
    }

    private fun attachActions() {
        flip(0)
        checkAndSetupInstall()
    }

    private fun attachToolbar() {
        binding.layoutDetailsToolbar.toolbar.apply {
            elevation = 0f
            navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back)
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
                    R.id.action_home_screen -> {
                        ShortcutManagerUtil.requestPinShortcut(
                            requireContext(),
                            app.packageName
                        )
                    }

                    R.id.action_share -> {
                        requireContext().share(app)
                    }

                    R.id.action_favourite -> {
                        viewModel.toggleFavourite(app)
                    }

                    R.id.action_uninstall -> {
                        AppInstaller.uninstall(requireContext(), app.packageName)
                    }

                    R.id.menu_download_manual -> {
                        findNavController().navigate(
                            AppDetailsFragmentDirections
                                .actionAppDetailsFragmentToManualDownloadSheet(app)
                        )
                    }

                    R.id.menu_app_settings -> {
                        val intent = Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", app.packageName, null)
                        }

                        startActivity(intent)
                    }

                    R.id.menu_download_manager -> {
                        findNavController().navigate(R.id.downloadFragment)
                    }

                    R.id.action_playstore -> {
                        requireContext().browse("${Constants.SHARE_URL}${app.packageName}")
                    }
                }
                true
            }

            if (::app.isInitialized) {
                app.isInstalled = PackageUtil.isInstalled(requireContext(), app.packageName)

                menu?.findItem(R.id.action_home_screen)?.isVisible =
                    app.isInstalled && ShortcutManagerUtil.canPinShortcut(
                        requireContext(),
                        app.packageName
                    )

                menu?.findItem(R.id.action_uninstall)?.isVisible = app.isInstalled
                menu?.findItem(R.id.menu_app_settings)?.isVisible = app.isInstalled
                uninstallActionEnabled = app.isInstalled
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

    private fun updateActionState(state: State) {
        runOnUiThread {
            binding.layoutDetailsInstall.btnDownload.apply {
                updateState(state)
                if (state == State.INSTALLING) {
                    setButtonState(false)
                    setText(R.string.action_installing)
                }
            }
        }
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

    @Synchronized
    private fun startDownload() {
        when (downloadStatus) {
            DownloadStatus.DOWNLOADING -> {
                flip(1)
                toast("Already downloading")
            }

            else -> {
                flip(1)
                purchase()
            }
        }
    }

    private fun purchase() {
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        updateActionState(State.PROGRESS)

        if (PathUtil.needsStorageManagerPerm(app.fileList)) {
            if (permissionProvider.isGranted(PermissionType.STORAGE_MANAGER)) {
                viewModel.download(app)
            } else {
                permissionProvider.request(PermissionType.STORAGE_MANAGER)
            }
        } else {
            viewModel.download(app)
        }
    }

    private fun updateProgress(progress: Int, speed: Long = -1, timeRemaining: Long = -1) {
        runOnUiThread {
            if (progress == 100) {
                binding.layoutDetailsInstall.btnDownload.setText(getString(R.string.action_installing))
                return@runOnUiThread
            }

            binding.layoutDetailsInstall.apply {
                txtProgressPercent.text = ("${progress}%")
                progressDownload.apply {
                    this.progress = progress
                    isIndeterminate = progress < 1
                }
                txtEta.text = CommonUtil.getETAString(requireContext(), timeRemaining)
                txtSpeed.text = CommonUtil.getDownloadSpeedString(requireContext(), speed)
            }
        }
    }

    private fun checkAndSetupInstall() {
        app.isInstalled = PackageUtil.isInstalled(requireContext(), app.packageName)

        runOnUiThread {
            binding.layoutDetailsInstall.btnDownload.let { btn ->
                btn.setButtonState(true)
                if (app.isInstalled) {
                    val isExtendedUpdateEnabled = Preferences.getBoolean(
                        requireContext(), Preferences.PREFERENCE_UPDATES_EXTENDED
                    )
                    val needsExtendedUpdate = !app.certificateSetList.any {
                        it.certificateSet in CertUtil.getEncodedCertificateHashes(
                            requireContext(), app.packageName
                        )
                    }
                    isUpdatable = PackageUtil.isUpdatable(
                        requireContext(),
                        app.packageName,
                        app.versionCode.toLong()
                    )

                    val installedVersion =
                        PackageUtil.getInstalledVersion(requireContext(), app.packageName)

                    if (isUpdatable && !needsExtendedUpdate || isUpdatable && isExtendedUpdateEnabled) {
                        binding.layoutDetailsApp.txtLine3.text =
                            ("$installedVersion ➔ ${app.versionName} (${app.versionCode})")
                        btn.setText(R.string.action_update)
                        btn.addOnClickListener {
                            if (app.versionCode == 0) {
                                toast(R.string.toast_app_unavailable)
                            } else {
                                startDownload()
                            }
                        }
                    } else {
                        binding.layoutDetailsApp.txtLine3.text = installedVersion
                        btn.setText(R.string.action_open)
                        btn.addOnClickListener { openApp() }
                    }
                    if (!uninstallActionEnabled) {
                        binding.layoutDetailsToolbar.toolbar.invalidateMenu()
                    }
                } else {
                    if (downloadStatus in DownloadStatus.running) {
                        flip(1)
                    } else if (app.isFree) {
                        btn.setText(R.string.action_install)
                    } else {
                        btn.setText(app.price)
                    }

                    btn.addOnClickListener {
                        if (!permissionProvider.isGranted(PermissionType.INSTALL_UNKNOWN_APPS)) {
                            permissionProvider.request(PermissionType.INSTALL_UNKNOWN_APPS)
                        } else if (authProvider.isAnonymous && !app.isFree) {
                            toast(R.string.toast_purchase_blocked)
                        } else if (app.versionCode == 0) {
                            toast(R.string.toast_app_unavailable)
                        } else {
                            val hasOBB = app.fileList.any { it.type == File.FileType.OBB }
                            if (hasOBB && !PathUtil.canReadWriteOBB()) {
                                permissionProvider.request(PermissionType.STORAGE_MANAGER)
                            } else {
                                btn.setText(R.string.download_metadata)
                                startDownload()
                            }
                        }
                    }

                    if (uninstallActionEnabled) {
                        binding.layoutDetailsToolbar.toolbar.invalidateMenu()
                    }
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

    private fun inflatePartialApp() {
        if (::app.isInitialized) {
            attachHeader()
            attachBottomSheet()
            attachActions()

            if (autoDownload) {
                purchase()
            }
        }
    }

    private fun inflateExtraDetails(app: App?) {
        app?.let {
            binding.viewFlipper.displayedChild = 1
            inflateAppDescription(app)
            inflateAppRatingAndReviews(app)
            inflateAppDevInfo(app)
            inflateAppDataSafety(app)
            inflateAppPrivacy(app)
            inflateAppPermission(app)

            if (!authProvider.isAnonymous) {
                app.testingProgram?.let {
                    if (it.isAvailable && it.isSubscribed) {
                        binding.layoutDetailsApp.txtLine1.text = it.displayName
                    }
                }

                inflateBetaSubscription(app)
            }

            if (Preferences.getBoolean(requireContext(), Preferences.PREFERENCE_SIMILAR)) {
                inflateAppStream(app)
            }
        }
    }

    private fun inflateAppDescription(app: App) {
        binding.layoutDetailDescription.apply {
            val installs = CommonUtil.addDiPrefix(app.installs)

            if (installs != "NA") {
                txtInstalls.text = CommonUtil.addDiPrefix(app.installs)
            } else {
                txtInstalls.hide()
            }

            txtSize.text = CommonUtil.addSiPrefix(app.size)
            txtRating.text = app.labeledRating
            txtSdk.text = ("Target SDK ${app.targetSdk}")
            txtUpdated.text = app.updatedOn
            txtDescription.text = HtmlCompat.fromHtml(
                app.shortDescription,
                HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS
            )

            app.changes.apply {
                if (isEmpty()) {
                    txtChangelog.text = getString(R.string.details_changelog_unavailable)
                } else {
                    txtChangelog.text = HtmlCompat.fromHtml(
                        this,
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                }
            }

            headerDescription.addClickListener {
                findNavController().navigate(
                    AppDetailsFragmentDirections.actionAppDetailsFragmentToDetailsMoreFragment(app)
                )
            }

            epoxyRecycler.withModels {
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
    }

    private fun inflateAppRatingAndReviews(app: App) {
        binding.layoutDetailsReview.apply {
            averageRating.text = app.rating.average.toString()
            txtReviewCount.text = app.rating.abbreviatedLabel

            var totalStars = 0L
            totalStars += app.rating.oneStar
            totalStars += app.rating.twoStar
            totalStars += app.rating.threeStar
            totalStars += app.rating.fourStar
            totalStars += app.rating.fiveStar

            avgRatingLayout.apply {
                removeAllViews()
                addView(addAvgReviews(5, totalStars, app.rating.fiveStar))
                addView(addAvgReviews(4, totalStars, app.rating.fourStar))
                addView(addAvgReviews(3, totalStars, app.rating.threeStar))
                addView(addAvgReviews(2, totalStars, app.rating.twoStar))
                addView(addAvgReviews(1, totalStars, app.rating.oneStar))
            }

            averageRating.text = String.format(Locale.getDefault(), "%.1f", app.rating.average)
            txtReviewCount.text = app.rating.abbreviatedLabel

            layoutUserReview.visibility =
                if (authProvider.isAnonymous) View.GONE else View.VISIBLE

            btnPostReview.setOnClickListener {
                if (authProvider.isAnonymous) {
                    toast(R.string.toast_anonymous_restriction)
                } else {
                    addOrUpdateReview(app, Review().apply {
                        title = inputTitle.text.toString()
                        rating = userStars.rating.toInt()
                        comment = inputReview.text.toString()
                    })
                }
            }

            headerRatingReviews.addClickListener {
                findNavController().navigate(
                    AppDetailsFragmentDirections.actionAppDetailsFragmentToDetailsReviewFragment(
                        app.displayName,
                        app.packageName
                    )
                )
            }
        }
    }

    private fun inflateAppDataSafety(app: App) {
        viewModel.fetchAppDataSafetyReport(app.packageName)
    }

    private fun inflateAppPrivacy(app: App) {
        viewModel.fetchAppReport(app.packageName)
    }

    private fun inflateAppDevInfo(app: App) {
        binding.layoutDetailsDev.apply {
            if (app.developerAddress.isNotEmpty()) {
                devAddress.apply {
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
                devWeb.apply {
                    setTxtSubtitle(app.developerWebsite)
                    visibility = View.VISIBLE
                }
            }

            if (app.developerEmail.isNotEmpty()) {
                devMail.apply {
                    setTxtSubtitle(app.developerEmail)
                    visibility = View.VISIBLE
                }
            }
        }
    }

    private fun inflateBetaSubscription(app: App) {
        binding.layoutDetailsBeta.apply {
            app.testingProgram?.let { betaProgram ->
                if (betaProgram.isAvailable) {
                    root.show()

                    updateBetaActions(betaProgram.isSubscribed)

                    if (betaProgram.isSubscribedAndInstalled) {

                    }

                    imgBeta.load(betaProgram.artwork.url) {

                    }

                    btnBetaAction.setOnClickListener {
                        btnBetaAction.text = getString(R.string.action_pending)
                        btnBetaAction.isEnabled = false
                        viewModel.fetchTestingProgramStatus(
                            app.packageName,
                            !betaProgram.isSubscribed
                        )
                    }
                } else {
                    root.hide()
                }
            }
        }
    }

    private fun inflateAppStream(app: App) {
        app.detailsStreamUrl?.let {
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
                        detailsClusterViewModel.observeCluster(
                            it,
                            streamCluster
                        )
                    }

                    override fun onAppClick(app: App) {
                        openDetailsFragment(app.packageName, app)
                    }

                    override fun onAppLongClick(app: App) {}
                })

            detailsClusterViewModel.liveData.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is ViewState.Success<*> -> {
                        val stash = state.getDataAs<AppStreamStash>()
                        streamBundle = stash[it]
                        carouselController.setData(streamBundle)
                    }

                    else -> {

                    }
                }
            }

            binding.epoxyRecyclerStream.setController(carouselController)
            detailsClusterViewModel.getStreamBundle(it)
        }
    }

    private fun inflateAppPermission(app: App) {
        binding.layoutDetailsPermissions.apply {
            headerPermission.addClickListener {
                if (app.permissions.isNotEmpty()) {
                    findNavController().navigate(
                        AppDetailsFragmentDirections.actionAppDetailsFragmentToPermissionBottomSheet(
                            app
                        )
                    )
                }
            }
            headerPermission.setSubTitle(("${app.permissions.size} permissions"))
        }
    }

    private fun updateBetaActions(isSubscribed: Boolean) {
        binding.layoutDetailsBeta.apply {
            if (isSubscribed) {
                btnBetaAction.text = getString(R.string.action_leave)
                headerRatingReviews.setSubTitle(getString(R.string.details_beta_subscribed))
            } else {
                btnBetaAction.text = getString(R.string.action_join)
                headerRatingReviews.setSubTitle(getString(R.string.details_beta_available))
            }
        }
    }

    private fun updateDataSafetyViews(report: DataSafetyReport) {
        report.entries.groupBy { it.type }.forEach { (type, entries) ->
            when (type) {
                EntryType.DATA_COLLECTED -> {
                    binding.layoutDetailsDataSafety.dataCollect.title = HtmlCompat.fromHtml(
                        entries.first().description,
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    ).toString()
                    binding.layoutDetailsDataSafety.dataCollect.subTitle =
                        entries.first().subEntries.joinToString(", ") { it.name }.ifBlank { null }
                }

                EntryType.DATA_SHARED -> {
                    binding.layoutDetailsDataSafety.dataShare.title = HtmlCompat.fromHtml(
                        entries.first().description,
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    ).toString()
                    binding.layoutDetailsDataSafety.dataShare.subTitle =
                        entries.first().subEntries.joinToString(", ") { it.name }.ifBlank { null }
                }

                else -> {}
            }
        }
    }

    /* App Review Helpers */

    private fun addAvgReviews(number: Int, max: Long, rating: Long): RelativeLayout {
        return RatingView(requireContext(), number, max.toInt(), rating.toInt())
    }

    private fun addOrUpdateReview(app: App, review: Review, isBeta: Boolean = false) {
        viewModel.postAppReview(app.packageName, review, isBeta)
    }
}
