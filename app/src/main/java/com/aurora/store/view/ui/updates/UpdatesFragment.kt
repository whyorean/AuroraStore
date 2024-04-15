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

package com.aurora.store.view.ui.updates

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.Constants
import com.aurora.extensions.browse
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.store.MobileNavigationDirections
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.room.download.Download
import com.aurora.store.databinding.FragmentUpdatesBinding
import com.aurora.store.util.PathUtil
import com.aurora.store.view.epoxy.views.UpdateHeaderViewModel_
import com.aurora.store.view.epoxy.views.app.AppUpdateViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.all.UpdatesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@AndroidEntryPoint
class UpdatesFragment : BaseFragment(R.layout.fragment_updates) {

    private lateinit var app: App

    private var _binding: FragmentUpdatesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UpdatesViewModel by viewModels()

    private val startForStorageManagerResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isRAndAbove() && Environment.isExternalStorageManager()) {
                viewModel.download(app)
            } else {
                toast(R.string.permissions_denied)
            }
        }
    private val startForPermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { perm ->
            if (perm) viewModel.download(app) else toast(R.string.permissions_denied)
        }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUpdatesBinding.bind(view)

        // Toolbar
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_download_manager -> {
                    findNavController().navigate(R.id.downloadFragment)
                }
                R.id.menu_more -> {
                    findNavController().navigate(
                        MobileNavigationDirections.actionGlobalMoreDialogFragment()
                    )
                }
            }
            true
        }

        viewModel.observe()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updates.combine(viewModel.downloadsList) { uList, dList ->
                uList?.associateWith { a ->
                    dList.find { it.packageName == a.packageName && it.versionCode == a.versionCode }
                }
            }.collectLatest { map ->
                updateController(map)
                binding.swipeRefreshLayout.isRefreshing = false
                viewModel.updateAllEnqueued = map?.values?.all { it?.isRunning == true } ?: false

                if (!map.isNullOrEmpty()) {
                    binding.updateFab.apply {
                        visibility = View.VISIBLE
                        if (viewModel.updateAllEnqueued) {
                            setImageDrawable(
                                ContextCompat.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_cancel
                                )
                            )
                        } else {
                            setImageDrawable(
                                ContextCompat.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_installation
                                )
                            )
                        }
                        setOnClickListener {
                            if (viewModel.updateAllEnqueued) {
                                cancelAll()
                            } else {
                                map.keys.forEach { updateSingle(it, true) }
                            }
                            binding.recycler.requestModelBuild()
                        }
                    }
                } else {
                    binding.updateFab.visibility = View.GONE
                }
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.observe()
        }

        updateController(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: Any) {
        when (event) {
            is BusEvent.InstallEvent, is BusEvent.UninstallEvent -> {
                viewModel.observe()
            }

            else -> {}
        }
    }

    private fun updateController(appList: Map<App, Download?>?) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            if (appList == null) {
                for (i in 1..10) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                if (appList.isEmpty()) {
                    add(
                        NoAppViewModel_()
                            .id("no_update")
                            .icon(R.drawable.ic_updates)
                            .message(getString(R.string.details_no_updates))
                    )
                } else {
                    add(
                        UpdateHeaderViewModel_()
                            .id("header_all")
                            .title(
                                "${appList.size} " +
                                        if (appList.size == 1)
                                            getString(R.string.update_available)
                                        else
                                            getString(R.string.updates_available)
                            )
                            .action(getString(R.string.action_manage))
                            .click { _ ->
                                findNavController().navigate(R.id.blacklistFragment)
                            }
                    )

                    for ((app, download) in appList) {
                        add(
                            AppUpdateViewModel_()
                                .id(app.packageName)
                                .app(app)
                                .download(download)
                                .click { _ ->
                                    if (app.packageName == Constants.APP_ID) {
                                        requireContext().browse(Constants.GITLAB_URL)
                                    } else {
                                        openDetailsFragment(app.packageName, app)
                                    }
                                }
                                .longClick { _ ->
                                    openAppMenuSheet(app)
                                    false
                                }
                                .positiveAction { _ -> updateSingle(app) }
                                .negativeAction { _ -> cancelSingle(app) }
                        )
                    }
                }
            }
        }
    }

    private fun updateSingle(app: App, updateAll: Boolean = false) {
        this.app = app
        viewModel.updateAllEnqueued = updateAll

        if (PathUtil.needsStorageManagerPerm(app.fileList)) {
            if (isRAndAbove()) {
                if (!Environment.isExternalStorageManager()) {
                    startForStorageManagerResult.launch(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
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

    private fun cancelSingle(app: App) {
        viewModel.cancelDownload(app)
    }

    private fun cancelAll() {
        viewModel.cancelAll()
    }
}
