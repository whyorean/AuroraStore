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

import android.animation.ObjectAnimator
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil3.asDrawable
import coil3.load
import coil3.request.error
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import coil3.transform.RoundedCornersTransformation
import com.aurora.Constants
import com.aurora.Constants.EXODUS_SUBMIT_PAGE
import com.aurora.extensions.browse
import com.aurora.extensions.hide
import com.aurora.extensions.invisible
import com.aurora.extensions.px
import com.aurora.extensions.requiresObbDir
import com.aurora.extensions.runOnUiThread
import com.aurora.extensions.share
import com.aurora.extensions.show
import com.aurora.extensions.toast
import com.aurora.extensions.updateText
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.data.models.datasafety.EntryType
import com.aurora.store.AppStreamStash
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.event.Event
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.model.ViewState.Loading.getDataAs
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.FragmentDetailsBinding
import com.aurora.store.util.CertUtil
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_SIMILAR
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_EXTENDED
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

    private lateinit var app: App
    private lateinit var iconDrawable: Drawable

    private var streamBundle: StreamBundle? = StreamBundle()

    private val isExternal get() = activity?.intent?.action != Intent.ACTION_MAIN

    private var downloadStatus = DownloadStatus.UNAVAILABLE
    private var isUpdatable: Boolean = false
    private var uninstallActionEnabled = false

    private val tags = mutableSetOf<String>()

    private val isExtendedUpdateEnabled: Boolean
        get() = Preferences.getBoolean(requireContext(), PREFERENCE_UPDATES_EXTENDED)
    private val showSimilarApps: Boolean
        get() = Preferences.getBoolean(requireContext(), PREFERENCE_SIMILAR)

    private fun onEvent(event: Event) {
        when (event) {
            is InstallerEvent.Installed -> {
                if (app.packageName == event.packageName) {
                    checkAndSetupInstall()
                    transformIcon(false)
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
                    checkAndSetupInstall()
                    transformIcon(false)
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
                    checkAndSetupInstall()
                }
            }

            else -> {

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        app = args.app ?: App(args.packageName)
        app.apply {
            // Check whether app is installed or not
            isInstalled = PackageUtil.isInstalled(requireContext(), app.packageName)
            uninstallActionEnabled = isInstalled
        }

        // Show the basic app details, while the rest of the data is being fetched
        updateAppHeader(app, false)

        // Toolbar
        attachToolbar(app)

        // App Details
        viewModel.fetchAppDetails(app.packageName)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.app.collect {
                if (it.packageName.isNotBlank()) {
                    app = it

                    // App User Review
                    // We can not fetch it outside of this block, as we need the testing program status
                    if (!authProvider.isAnonymous && app.isInstalled) {
                        viewModel.fetchUserAppReview(app)
                    }

                    updateAppHeader(app) // Re-inflate the app details, as web data may vary.
                    updateExtraDetails(app)

                    // Fetch App Reviews
                    viewModel.fetchAppReviews(app.packageName)

                    // Fetch Data Safety Report
                    viewModel.fetchAppDataSafetyReport(app.packageName)

                    // Fetch Exodus Privacy Report
                    viewModel.fetchAppReport(app.packageName)
                } else {
                    toast(getString(R.string.status_unavailable))
                    // TODO: Redirect to App Unavailable Fragment
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

                        when (it.downloadStatus) {
                            DownloadStatus.QUEUED -> {
                                updateProgress(it.progress)
                            }

                            DownloadStatus.DOWNLOADING -> {
                                updateSecondaryAction(true)
                                updateProgress(it.progress, it.speed, it.timeRemaining)
                            }

                            else -> {
                                transformIcon(false)
                                updateSecondaryAction(false)
                            }
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
                    runOnUiThread { updateUserReview(it) }
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

    private fun attachToolbar(app: App) {
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

            // Inflate Menu
            inflateMenu(R.menu.menu_details)

            // Adjust Menu Items
            menu.let {
                it.findItem(R.id.action_home_screen)?.isVisible =
                    app.isInstalled && ShortcutManagerUtil.canPinShortcut(
                        requireContext(),
                        app.packageName
                    )
                it.findItem(R.id.action_uninstall)?.isVisible = app.isInstalled
                it.findItem(R.id.menu_app_settings)?.isVisible = app.isInstalled
            }

            // Set Menu Item Clicks
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
        }
    }

    private fun updateAppHeader(app: App, isFullApp: Boolean = true) {
        binding.layoutDetailsApp.apply {
            val fallbackDrawable = if (app.iconArtwork.url.isNotBlank())
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_placeholder)
            else
                PackageUtil.getIconDrawableForPackage(requireContext(), app.packageName)

            imgIcon.load(app.iconArtwork.url) {
                error(fallbackDrawable)
                transformations(RoundedCornersTransformation(32F))
                listener { _, result ->
                    result.image.asDrawable(resources).let { iconDrawable = it }
                }
            }

            packageName.updateText(app.packageName)
            txtLine1.updateText(app.displayName)
            txtLine2.updateText(app.developerName)
            txtLine3.updateText(("${app.versionName} (${app.versionCode})"))

            txtLine2.setOnClickListener {
                findNavController().navigate(
                    AppDetailsFragmentDirections
                        .actionAppDetailsFragmentToDevAppsFragment(app.developerName)
                )
            }

            // Do not show tags for web apps or unknown apps
            if (isFullApp) {
                tags.add(getString((if (app.isFree) R.string.details_free else R.string.details_paid)))
                tags.add(getString((if (app.containsAds) R.string.details_contains_ads else R.string.details_no_ads)))
                txtLine4.updateText(tags.joinToString(separator = " • "))
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
                toast("Already downloading")
            }

            else -> {
                purchase()
            }
        }
    }

    private fun purchase() {
        if (app.fileList.requiresObbDir()) {
            if (permissionProvider.isGranted(PermissionType.STORAGE_MANAGER)) {
                viewModel.download(app)
            } else {
                permissionProvider.request(PermissionType.STORAGE_MANAGER) {
                    if (it) viewModel.download(app) else {
                        // TODO: Ask for permission again or redirect to Permission Manager
                    }
                }
            }
        } else {
            viewModel.download(app)
        }
    }

    private fun updateProgress(progress: Int, speed: Long = -1, timeRemaining: Long = -1) {
        runOnUiThread {
            updatePrimaryAction(false)
            updateSecondaryAction(true)

            if (progress == 100) {
                transformIcon(false)
                binding.layoutDetailsApp.apply {
                    txtLine3.text = ("${app.versionName} (${app.versionCode})")
                    txtLine4.text = tags.joinToString(separator = " • ")
                }
                return@runOnUiThread
            }

            transformIcon(true)
            binding.layoutDetailsApp.apply {
                if (progress < 1) {
                    progressDownload.isIndeterminate = true
                } else {
                    progressDownload.isIndeterminate = false
                    progressDownload.progress = progress
                    txtLine3.text = CommonUtil.getETAString(requireContext(), timeRemaining)
                    txtLine4.text = CommonUtil.getDownloadSpeedString(requireContext(), speed)
                }
            }
        }
    }

    private fun updatePrimaryAction(enabled: Boolean = false) {
        binding.layoutDetailsApp.btnPrimaryAction.apply {
            isEnabled = enabled
            text = if (app.isInstalled) {
                getString(R.string.action_open)
            } else {
                getString(R.string.action_install)
            }
        }
    }

    @Synchronized
    private fun updateSecondaryAction(enabled: Boolean = false) {
        runOnUiThread {
            binding.layoutDetailsApp.btnSecondaryAction.apply {
                isEnabled = enabled
                isVisible = enabled
                text = getString(R.string.action_cancel)
                setOnClickListener {
                    viewModel.cancelDownload(app)
                    updatePrimaryAction(true)
                }
            }
        }
    }

    private fun transformIcon(ongoing: Boolean = false) {
        if (::iconDrawable.isInitialized.not()) return

        val scaleFactor = if (ongoing) 0.75f else 1f
        val isDownloadVisible = binding.layoutDetailsApp.progressDownload.isShown

        // Avoids flickering when the download is in progress
        if (isDownloadVisible && scaleFactor != 1f)
            return

        if (!isDownloadVisible && scaleFactor == 1f)
            return

        if (scaleFactor == 1f) {
            binding.layoutDetailsApp.progressDownload.invisible()
        } else {
            binding.layoutDetailsApp.progressDownload.show()
        }

        val scale = listOf(
            ObjectAnimator.ofFloat(binding.layoutDetailsApp.imgIcon, "scaleX", scaleFactor),
            ObjectAnimator.ofFloat(binding.layoutDetailsApp.imgIcon, "scaleY", scaleFactor)
        )

        scale.forEach { animation ->
            animation.apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = 250
                start()
            }
        }

        iconDrawable.let {
            binding.layoutDetailsApp.imgIcon.load(it) {
                transformations(
                    if (scaleFactor == 1f)
                        RoundedCornersTransformation(8.px.toFloat())
                    else
                        CircleCropTransformation()
                )
            }
        }
    }

    @Synchronized
    private fun checkAndSetupInstall() {
        runOnUiThread {
            app.isInstalled = PackageUtil.isInstalled(requireContext(), app.packageName)

            if (app.isInstalled) {
                val needsExtendedUpdate = !app.certificateSetList.any {
                    it.certificateSet in CertUtil.getEncodedCertificateHashes(
                        requireContext(),
                        app.packageName
                    )
                }

                isUpdatable = PackageUtil.isUpdatable(
                    requireContext(),
                    app.packageName,
                    app.versionCode.toLong()
                )

                val installedVersion = PackageUtil.getInstalledVersion(
                    requireContext(),
                    app.packageName
                )

                if ((isUpdatable && !needsExtendedUpdate) || (isUpdatable && isExtendedUpdateEnabled)) {
                    binding.layoutDetailsApp.apply {
                        txtLine3.text =
                            ("$installedVersion ➔ ${app.versionName} (${app.versionCode})")
                        txtLine4.text = tags.joinToString(separator = " • ")
                        btnPrimaryAction.apply {
                            isEnabled = true
                            setText(R.string.action_update)
                            setOnClickListener {
                                if (app.versionCode == 0) {
                                    toast(R.string.toast_app_unavailable)
                                    btnPrimaryAction.setText(R.string.status_unavailable)
                                } else {
                                    startDownload()
                                }
                            }
                        }
                    }
                } else {
                    binding.layoutDetailsApp.apply {
                        txtLine3.text = installedVersion
                        btnPrimaryAction.apply {
                            isEnabled = true
                            setText(R.string.action_open)
                            setOnClickListener { openApp() }
                        }
                    }
                }

                if (!uninstallActionEnabled) {
                    binding.layoutDetailsToolbar.toolbar.invalidateMenu()
                }
            } else {
                if (downloadStatus in DownloadStatus.running) {
                    updateProgress(-1)
                } else if (app.isFree) {
                    binding.layoutDetailsApp.btnPrimaryAction.setText(R.string.action_install)
                } else {
                    binding.layoutDetailsApp.btnPrimaryAction.text = app.price
                }

                binding.layoutDetailsApp.btnPrimaryAction.setOnClickListener {
                    if (authProvider.isAnonymous && !app.isFree) {
                        toast(R.string.toast_purchase_blocked)
                        return@setOnClickListener
                    } else if (app.versionCode == 0) {
                        toast(R.string.toast_app_unavailable)
                        return@setOnClickListener
                    }

                    if (!permissionProvider.isGranted(PermissionType.INSTALL_UNKNOWN_APPS)) {
                        permissionProvider.request(PermissionType.INSTALL_UNKNOWN_APPS) {
                            if (it) {
                                startDownload()
                            } else {
                                toast(R.string.permissions_denied)
                                // TODO: Warn & redirect to Permission Manager
                            }
                        }
                    } else {
                        startDownload()
                    }
                }

                if (uninstallActionEnabled) {
                    binding.layoutDetailsToolbar.toolbar.invalidateMenu()
                }
            }
        }
    }

    private fun updateExtraDetails(app: App) {
        binding.viewFlipper.displayedChild = 1

        updateAppDescription(app)
        updateAppRatingAndReviews(app)
        updateAppDevInfo(app)
        updateAppPermission(app)

        // Allow users to handle beta subscriptions, if logged in by own account.
        if (!authProvider.isAnonymous) {
            // Update app name to the testing program name, if subscribed
            app.testingProgram?.let {
                if (it.isAvailable && it.isSubscribed) {
                    binding.layoutDetailsApp.txtLine1.text = it.displayName
                }
            }

            updateBetaSubscription(app)
        }

        if (showSimilarApps) {
            updateAppStream(app)
        }

        checkAndSetupInstall()
    }

    private fun updateAppDescription(app: App) {
        binding.layoutDetailDescription.apply {
            val installs = CommonUtil.addDiPrefix(app.installs)

            if (installs != "NA") {
                txtInstalls.text = CommonUtil.addDiPrefix(app.installs)
            } else {
                txtInstalls.hide()
            }

            txtSize.text = CommonUtil.addSiPrefix(app.size)
            txtRating.text = app.labeledRating
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

    private fun updateAppRatingAndReviews(app: App) {
        binding.layoutDetailsReview.apply {
            headerRatingReviews.addClickListener {
                findNavController().navigate(
                    AppDetailsFragmentDirections.actionAppDetailsFragmentToDetailsReviewFragment(
                        app.displayName,
                        app.packageName
                    )
                )
            }

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
        }
    }

    private fun updateUserReview(review: Review) {
        binding.layoutDetailsReview.apply {
            layoutUserReview.visibility = View.VISIBLE
            inputTitle.setText(review.title)
            inputReview.setText(review.comment)
            userStars.rating = review.rating.toFloat()

            if (!authProvider.isAnonymous && app.isInstalled) {
                btnPostReview.setOnClickListener {
                    addOrUpdateReview(
                        app,
                        Review().apply {
                            title = inputTitle.text.toString()
                            rating = userStars.rating.toInt()
                            comment = inputReview.text.toString()
                        }
                    )
                }
            } else {
                layoutUserReview.visibility = View.GONE
            }
        }
    }

    private fun updateAppDevInfo(app: App) {
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

    private fun updateBetaSubscription(app: App) {
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

    private fun updateAppStream(app: App) {
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

    private fun updateAppPermission(app: App) {
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
