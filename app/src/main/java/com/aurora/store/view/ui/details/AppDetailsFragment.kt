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
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import coil.transform.RoundedCornersTransformation
import com.airbnb.epoxy.EpoxyRecyclerView
import com.aurora.Constants
import com.aurora.Constants.EXODUS_SUBMIT_PAGE
import com.aurora.extensions.browse
import com.aurora.extensions.getString
import com.aurora.extensions.hide
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.runOnUiThread
import com.aurora.extensions.share
import com.aurora.extensions.show
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.model.State
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.event.Event
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.FragmentDetailsBinding
import com.aurora.store.databinding.LayoutDetailsBetaBinding
import com.aurora.store.databinding.LayoutDetailsDescriptionBinding
import com.aurora.store.databinding.LayoutDetailsDevBinding
import com.aurora.store.databinding.LayoutDetailsPermissionsBinding
import com.aurora.store.databinding.LayoutDetailsReviewBinding
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

@AndroidEntryPoint
class AppDetailsFragment : BaseFragment(R.layout.fragment_details) {

    private var _binding: FragmentDetailsBinding? = null
    private val binding: FragmentDetailsBinding
        get() = _binding!!

    private val viewModel: AppDetailsViewModel by viewModels()
    private val detailsClusterViewModel: DetailsClusterViewModel by viewModels()

    private val args: AppDetailsFragmentArgs by navArgs()

    @Inject
    lateinit var authProvider: AuthProvider

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private val startForStorageManagerResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isRAndAbove() && Environment.isExternalStorageManager()) {
                viewModel.download(app)
            } else {
                flip(0)
                toast(R.string.permissions_denied)
            }
        }
    private val startForPermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                viewModel.download(app)
            } else {
                flip(0)
                toast(R.string.permissions_denied)
            }
        }

    private lateinit var app: App

    private var isExternal = false
    private var downloadStatus = DownloadStatus.UNAVAILABLE
    private var isUpdatable: Boolean = false
    private var autoDownload: Boolean = false
    private var uninstallActionEnabled = false

    private fun onEvent(event: Event) {
        when (event) {
            is BusEvent.InstallEvent -> {
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

            is BusEvent.UninstallEvent -> {
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

        if (args.app != null) {
            app = args.app!!
            inflatePartialApp()
        } else {
            isExternal = true
            app = App(args.packageName)
        }

        // Check whether app is installed or not
        app.isInstalled = PackageUtil.isInstalled(requireContext(), app.packageName)

        // App Details
        viewModel.fetchAppDetails(view.context, app.packageName)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.app.collect {
                if (it.packageName.isNotBlank()) {
                    app = it
                    inflatePartialApp() // Re-inflate the app details, as web data may vary.
                    inflateExtraDetails(app)
                    viewModel.fetchAppReviews(view.context, app.packageName)
                } else {
                    toast("Failed to fetch app details")
                }
            }
        }

        // Downloads
        binding.layoutDetailsInstall.progressDownload.clipToOutline = true
        binding.layoutDetailsInstall.imgCancel.setOnClickListener {
            viewModel.cancelDownload(app)
            if (downloadStatus != DownloadStatus.DOWNLOADING) flip(0)
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

        // Report (Exodus Privacy)
        binding.layoutDetailsPrivacy.btnRequestAnalysis.setOnClickListener {
            it.context.browse("${EXODUS_SUBMIT_PAGE}${app.packageName}")
        }
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
                    R.id.action_home_screen -> {
                        ShortcutManagerUtil.requestPinShortcut(
                            requireContext(),
                            app.packageName
                        )
                    }

                    R.id.action_share -> {
                        view.context.share(app)
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
                        view.context.browse("${Constants.SHARE_URL}${app.packageName}")
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

        viewLifecycleOwner.lifecycleScope.launch {
            AuroraApp.flowEvent.busEvent.collect { onEvent(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            AuroraApp.flowEvent.installerEvent.collect { onEvent(it) }
        }
    }

    override fun onResume() {
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

            if (!authProvider.isAnonymous) {
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
            if (isRAndAbove()) {
                if (!Environment.isExternalStorageManager()) {
                    startForStorageManagerResult.launch(
                        PackageUtil.getStorageManagerIntent(requireContext())
                    )
                } else {
                    viewModel.download(app)
                }
            } else {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.download(app)
                } else {
                    startForPermissions.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
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
            app.isInstalled = PackageUtil.isInstalled(requireContext(), app.packageName)

            binding.layoutDetailsInstall.btnDownload.let { btn ->
                if (app.isInstalled) {
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
                        if (authProvider.isAnonymous && !app.isFree) {
                            toast(R.string.toast_purchase_blocked)
                        } else if (app.versionCode == 0) {
                            toast(R.string.toast_app_unavailable)
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

        B.layoutUserReview.visibility = if (authProvider.isAnonymous) View.GONE else View.VISIBLE

        B.btnPostReview.setOnClickListener {
            if (authProvider.isAnonymous) {
                toast(R.string.toast_anonymous_restriction)
            } else {
                addOrUpdateReview(app, Review().apply {
                    title = B.inputTitle.text.toString()
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
        viewModel.fetchAppReport(requireContext(), app.packageName)
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
                        detailsClusterViewModel.observeCluster(streamCluster)
                    }

                    override fun onAppClick(app: App) {
                        openDetailsFragment(app.packageName, app)
                    }

                    override fun onAppLongClick(app: App) {

                    }
                })

            detailsClusterViewModel.liveData.observe(viewLifecycleOwner) {
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

            detailsClusterViewModel.getStreamBundle(it)
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
