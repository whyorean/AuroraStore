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
import com.aurora.extensions.px
import com.aurora.extensions.requiresObbDir
import com.aurora.extensions.share
import com.aurora.extensions.show
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.data.models.datasafety.EntryType
import com.aurora.store.AppStreamStash
import com.aurora.store.AuroraApp
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.event.Event
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.model.ViewState.Loading.getDataAs
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
import com.aurora.store.view.epoxy.views.details.ScreenshotView
import com.aurora.store.view.epoxy.views.details.ScreenshotViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.details.DetailsClusterViewModel
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Locale
import com.aurora.gplayapi.data.models.datasafety.Report as DataSafetyReport

@AndroidEntryPoint
class AppDetailsFragment : BaseFragment<FragmentDetailsBinding>() {

    private val viewModel: AppDetailsViewModel by activityViewModels()
    private val detailsClusterViewModel: DetailsClusterViewModel by activityViewModels()

    private val args: AppDetailsFragmentArgs by navArgs()

    private lateinit var app: App
    private lateinit var iconDrawable: Drawable

    private var streamBundle: StreamBundle? = StreamBundle()

    private val isExternal get() = activity?.intent?.action != Intent.ACTION_MAIN

    private val isExtendedUpdateEnabled: Boolean
        get() = Preferences.getBoolean(requireContext(), PREFERENCE_UPDATES_EXTENDED)
    private val showSimilarApps: Boolean
        get() = Preferences.getBoolean(requireContext(), PREFERENCE_SIMILAR)

    private fun onEvent(event: Event) {
        when (event) {
            is InstallerEvent.Installed -> {
                if (app.packageName == event.packageName) {
                    checkAndSetupInstall()
                }
            }

            is InstallerEvent.Uninstalled -> {
                if (app.packageName == event.packageName) {
                    checkAndSetupInstall()
                }
            }

            is BusEvent.ManualDownload -> {
                if (app.packageName == event.packageName) {
                    val requestedApp = app.copy(
                        versionCode = event.versionCode,
                        dependencies = app.dependencies.copy(
                            dependentLibraries = app.dependencies.dependentLibraries.onEach { lib ->
                                lib.versionCode = event.versionCode
                            }
                        )
                    )
                    purchase(requestedApp)
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
        }

        // Show the basic app details, while the rest of the data is being fetched
        updateAppHeader(app, false)

        // Toolbar
        updateToolbar(app)

        viewModel.download.filterNotNull().onEach {
            when (it.downloadStatus) {
                DownloadStatus.QUEUED,
                DownloadStatus.DOWNLOADING -> {
                    updateProgress(it.progress)
                    binding.layoutDetailsApp.btnPrimaryAction.apply {
                        isEnabled = false
                        text = getString(R.string.action_open)
                        setOnClickListener(null)
                    }
                    binding.layoutDetailsApp.btnSecondaryAction.apply {
                        isEnabled = true
                        text = getString(R.string.action_cancel)
                        setOnClickListener { viewModel.cancelDownload(app) }
                    }
                }

                DownloadStatus.VERIFYING -> {
                    transformIcon(true)
                    binding.layoutDetailsApp.btnPrimaryAction.apply {
                        isEnabled = false
                        text = getString(R.string.action_open)
                        setOnClickListener(null)
                    }
                    binding.layoutDetailsApp.btnSecondaryAction.apply {
                        isEnabled = false
                        text = getString(R.string.action_cancel)
                        setOnClickListener(null)
                    }
                }

                else -> checkAndSetupInstall()
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

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
                    binding.toolbar.menu?.findItem(R.id.action_favourite)
                        ?.setIcon(R.drawable.ic_favorite_checked)
                } else {
                    binding.toolbar.menu?.findItem(R.id.action_favourite)
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

    private fun updateToolbar(app: App) {
        binding.toolbar.apply {
            elevation = 0f
            navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back)

            setNavigationOnClickListener {
                if (isExternal) {
                    activity?.finish()
                } else {
                    findNavController().navigateUp()
                }
            }

            if (menu.size() == 0) {
                // Inflate Menu only if it is not already inflated
                inflateMenu(R.menu.menu_details)
            }

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

            packageName.text = app.packageName
            txtLine1.text = app.displayName
            txtLine2.text = app.developerName
            txtLine3.text = getString(R.string.version, app.versionName, app.versionCode)

            txtLine2.setOnClickListener {
                findNavController().navigate(
                    AppDetailsFragmentDirections
                        .actionAppDetailsFragmentToDevAppsFragment(app.developerName)
                )
            }

            // Do not show tags for web apps or unknown apps
            if (isFullApp) {
                val tags = mutableSetOf<String>().apply {
                    if (app.isFree) {
                        add(getString(R.string.details_free))
                    } else {
                        add(getString(R.string.details_paid))
                    }

                    if (app.containsAds) {
                        add(getString(R.string.details_contains_ads))
                    } else {
                        add(getString(R.string.details_no_ads))
                    }
                }

                txtLine4.text = tags.joinToString(separator = " • ")
            }
        }
    }

    private fun purchase(app: App) {
        if (app.fileList.requiresObbDir()) {
            if (permissionProvider.isGranted(PermissionType.STORAGE_MANAGER)) {
                viewModel.download(app)
            } else {
                permissionProvider.request(PermissionType.STORAGE_MANAGER) {
                    if (it) {
                        // Restart the app to ensure all permissions are granted
                        val intent = Intent(
                            requireContext(),
                            MainActivity::class.java
                        )
                        // Pass the packageName so we're back to same app
                        intent.putExtra("packageName", app.packageName)

                        ProcessPhoenix.triggerRebirth(requireContext(), intent)
                    }
                }
            }
        } else {
            viewModel.download(app)
        }
    }

    private fun updateProgress(progress: Int) {
        // No need to update progress if it is already 100% / completed
        transformIcon(progress != 100)
        if (progress == 100) return

        binding.layoutDetailsApp.apply {
            progressDownload.progress = progress
            progressDownload.isIndeterminate = progress < 1
        }
    }

    private fun transformIcon(ongoing: Boolean = false) {
        if (::iconDrawable.isInitialized.not()) return

        val imgIcon = binding.layoutDetailsApp.imgIcon
        val progressDownload = binding.layoutDetailsApp.progressDownload

        // Avoids flickering when the download is in progress
        if (progressDownload.isShown && ongoing) return
        if (!progressDownload.isShown && !ongoing) return

        binding.layoutDetailsApp.progressDownload.isVisible = ongoing

        val scaleFactor = if (ongoing) 0.75f else 1f
        val scale = listOf(
            ObjectAnimator.ofFloat(imgIcon, "scaleX", scaleFactor),
            ObjectAnimator.ofFloat(imgIcon, "scaleY", scaleFactor)
        )

        scale.forEach { animation ->
            animation.apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = 250
                start()
            }
        }

        imgIcon.load(iconDrawable) {
            transformations(
                if (ongoing) {
                    CircleCropTransformation()
                } else {
                    RoundedCornersTransformation(8.px.toFloat())
                }
            )
        }
    }

    private fun checkAndSetupInstall() {
        app.isInstalled = PackageUtil.isInstalled(requireContext(), app.packageName)

        // Setup primary and secondary action buttons
        binding.layoutDetailsApp.btnPrimaryAction.isEnabled = true
        binding.layoutDetailsApp.btnSecondaryAction.isEnabled = true

        if (app.isInstalled) {
            val isUpdatable =
                PackageUtil.isUpdatable(requireContext(), app.packageName, app.versionCode.toLong())
            val hasValidCert = app.certificateSetList.any {
                it.certificateSet in CertUtil.getEncodedCertificateHashes(
                    requireContext(),
                    app.packageName
                )
            }

            if ((isUpdatable && hasValidCert) || (isUpdatable && isExtendedUpdateEnabled)) {
                binding.layoutDetailsApp.txtLine3.text = getString(
                    R.string.version_update,
                    PackageUtil.getInstalledVersionName(requireContext(), app.packageName),
                    PackageUtil.getInstalledVersionCode(requireContext(), app.packageName),
                    app.versionName,
                    app.versionCode
                )
                binding.layoutDetailsApp.btnPrimaryAction.apply {
                    text = getString(R.string.action_update)
                    setOnClickListener {
                        if (app.versionCode == 0) {
                            toast(R.string.toast_app_unavailable)
                            setText(R.string.status_unavailable)
                        } else {
                            purchase(app)
                        }
                    }
                }
            } else {
                binding.layoutDetailsApp.apply {
                    txtLine3.text = getString(
                        R.string.version,
                        PackageUtil.getInstalledVersionName(requireContext(), app.packageName),
                        PackageUtil.getInstalledVersionCode(requireContext(), app.packageName)
                    )
                    btnPrimaryAction.apply {
                        val intent = PackageUtil.getLaunchIntent(requireContext(), app.packageName)
                        setText(R.string.action_open)
                        if (intent != null) {
                            isEnabled = true
                            setOnClickListener {
                                try {
                                    startActivity(intent)
                                } catch (exception: ActivityNotFoundException) {
                                    toast(getString(R.string.unable_to_open))
                                }
                            }
                        } else {
                            isEnabled = false
                            setOnClickListener(null)
                        }
                    }
                }
            }

            binding.layoutDetailsApp.btnSecondaryAction.apply {
                text = getString(R.string.action_uninstall)
                setOnClickListener {
                    AppInstaller.uninstall(requireContext(), app.packageName)
                }
            }
        } else {
            if (PackageUtil.isArchived(requireContext(), app.packageName)) {
                binding.layoutDetailsApp.btnPrimaryAction.text =
                    getString(R.string.action_unarchive)
            } else {
                if (app.isFree) {
                    binding.layoutDetailsApp.btnPrimaryAction.text =
                        getString(R.string.action_install)
                } else {
                    binding.layoutDetailsApp.btnPrimaryAction.text = app.price
                }
            }

            binding.layoutDetailsApp.btnPrimaryAction.setOnClickListener {
                if (viewModel.authProvider.isAnonymous && !app.isFree) {
                    toast(R.string.toast_purchase_blocked)
                    return@setOnClickListener
                } else if (app.versionCode == 0) {
                    toast(R.string.toast_app_unavailable)
                    return@setOnClickListener
                }

                if (!permissionProvider.isGranted(PermissionType.INSTALL_UNKNOWN_APPS)) {
                    permissionProvider.request(PermissionType.INSTALL_UNKNOWN_APPS) {
                        if (it) {
                            purchase(app)
                        } else {
                            toast(R.string.permissions_denied)
                        }
                    }
                } else {
                    purchase(app)
                }
            }

            binding.layoutDetailsApp.btnSecondaryAction.apply {
                text = getString(R.string.title_manual_download)
                setOnClickListener {
                    findNavController().navigate(
                        AppDetailsFragmentDirections
                            .actionAppDetailsFragmentToManualDownloadSheet(app)
                    )
                }
            }
        }

        // Lay out the toolbar again
        binding.toolbar.invalidateMenu()

        if (app.isInstalled) {
            binding.toolbar.menu.apply {
                findItem(R.id.action_home_screen)?.isVisible =
                    ShortcutManagerUtil.canPinShortcut(requireContext(), app.packageName)
                findItem(R.id.action_uninstall)?.isVisible = true
                findItem(R.id.menu_app_settings)?.isVisible = true
            }
        } else {
            binding.toolbar.menu.apply {
                findItem(R.id.action_home_screen)?.isVisible = false
                findItem(R.id.action_uninstall)?.isVisible = false
                findItem(R.id.menu_app_settings)?.isVisible = false
            }
        }

        // Restore icon and progress
        updateProgress(100)
    }

    private fun updateExtraDetails(app: App) {
        binding.viewFlipper.displayedChild = 1

        updateAppDescription(app)
        updateAppRatingAndReviews(app)
        updateUserReview()
        updateAppDevInfo(app)
        updateAppPermission(app)

        // Allow users to handle beta subscriptions, if logged in by own account.
        if (!viewModel.authProvider.isAnonymous) {
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

            if (installs != null) {
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

    private fun updateUserReview(review: Review? = null) {
        binding.layoutDetailsReview.apply {
            layoutUserReview.isVisible = !viewModel.authProvider.isAnonymous
            btnPostReview.setOnClickListener {
                viewModel.postAppReview(
                    app.packageName,
                    Review(
                        title = inputTitle.text.toString(),
                        rating = userStars.rating.toInt(),
                        comment = inputReview.text.toString()
                    ),
                    app.testingProgram?.isSubscribed ?: false
                )
            }

            if (review != null) {
                inputTitle.setText(review.title)
                inputReview.setText(review.comment)
                userStars.rating = review.rating.toFloat()
            }
        }
    }

    private fun updateAppDevInfo(app: App) {
        binding.layoutDetailsDev.apply {
            if (app.developerAddress.isNotEmpty()) {
                devAddress.apply {
                    subTitle = HtmlCompat.fromHtml(
                        app.developerAddress,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ).toString()
                    visibility = View.VISIBLE
                }
            }

            if (app.developerWebsite.isNotEmpty()) {
                devWeb.apply {
                    subTitle = app.developerWebsite
                    visibility = View.VISIBLE
                }
            }

            if (app.developerEmail.isNotEmpty()) {
                devMail.apply {
                    subTitle = app.developerEmail
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

    private fun updateCompatibilityInfo() {
        if (app.dependencies.dependentPackages.contains(PackageUtil.PACKAGE_NAME_GMS)) {
            viewModel.fetchPlexusReport(app.packageName)

            binding.layoutDetailsCompatibility.txtGmsDependency.apply {
                title = getString(R.string.details_compatibility_gms_required_title)
                subTitle = getString(R.string.details_compatibility_gms_required_subtitle)
                titleColor = ContextCompat.getColor(context, R.color.colorRed)
            }

            binding.layoutDetailsCompatibility.compatibilityStatusLayout.isVisible = true
        } else {
            binding.layoutDetailsCompatibility.txtGmsDependency.apply {
                title = getString(R.string.details_compatibility_gms_not_required_title)
                subTitle = getString(R.string.details_compatibility_gms_not_required_subtitle)
                titleColor = ContextCompat.getColor(context, R.color.colorRed)
            }
        }
    }

    private fun warnAppUnavailable(app: App) {
        AuroraApp.events.send(InstallerEvent.Failed(app.packageName).apply {
            error = getString(R.string.status_unavailable)
            extra = getString(R.string.toast_app_unavailable)
        })
    }

    /* App Review Helpers */

    private fun addAvgReviews(number: Int, max: Long, rating: Long): RelativeLayout {
        return RatingView(requireContext(), number, max.toInt(), rating.toInt())
    }
}
