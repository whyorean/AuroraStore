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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.Constants
import com.aurora.extensions.browse
import com.aurora.store.MobileNavigationDirections
import com.aurora.store.PermissionType
import com.aurora.store.R
import com.aurora.store.data.model.MinimalApp
import com.aurora.store.data.providers.PermissionProvider
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.update.Update
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

@AndroidEntryPoint
class UpdatesFragment : BaseFragment<FragmentUpdatesBinding>() {

    private lateinit var permissionProvider: PermissionProvider
    private lateinit var update: Update

    private val viewModel: UpdatesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionProvider = PermissionProvider(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updates
                .combine(viewModel.downloadsList) { uList, dList ->
                    uList?.associateWith { a ->
                        dList.find { it.packageName == a.packageName && it.versionCode == a.versionCode }
                    }
                }.collectLatest { map ->
                    updateController(map)
                    viewModel.updateAllEnqueued =
                        map?.values?.all { it?.isRunning == true } ?: false
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.fetchingUpdates.collect {
                binding.swipeRefreshLayout.isRefreshing = it
                if (it && viewModel.updates.value.isNullOrEmpty()) {
                    updateController(emptyMap())
                }
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchUpdates()
        }

        binding.searchFab.setOnClickListener {
            findNavController().navigate(R.id.searchSuggestionFragment)
        }
    }

    override fun onDestroy() {
        permissionProvider.unregister()
        super.onDestroy()
    }

    private fun updateController(appList: Map<Update, Download?>?) {
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
                            .showAction(true)
                            .actionMessage(getString(R.string.check_updates))
                            .actionCallback { _ -> viewModel.fetchUpdates() }
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
                            .action(
                                if (viewModel.updateAllEnqueued) {
                                    getString(R.string.action_cancel)
                                } else {
                                    getString(R.string.action_update_all)
                                }
                            )
                            .click { _ ->
                                if (viewModel.updateAllEnqueued) {
                                    cancelAll()
                                } else {
                                    appList.keys.forEach { updateSingle(it, true) }
                                }
                                requestModelBuild()
                            }
                    )

                    for ((update, download) in appList) {
                        add(
                            AppUpdateViewModel_()
                                .id(update.packageName)
                                .update(update)
                                .download(download)
                                .click { _ ->
                                    if (update.packageName == Constants.APP_ID) {
                                        requireContext().browse(Constants.GITLAB_URL)
                                    } else {
                                        openDetailsFragment(update.packageName)
                                    }
                                }
                                .longClick { _ ->
                                    openAppMenuSheet(MinimalApp.fromUpdate(update))
                                    false
                                }
                                .positiveAction { _ -> updateSingle(update) }
                                .negativeAction { _ -> cancelSingle(update) }
                        )
                    }
                }
            }
        }
    }

    private fun updateSingle(update: Update, updateAll: Boolean = false) {
        this.update = update
        viewModel.updateAllEnqueued = updateAll

        if (PathUtil.needsStorageManagerPerm(update.fileList)) {
            if (permissionProvider.isGranted(PermissionType.STORAGE_MANAGER)) {
                viewModel.download(update)
            } else {
                permissionProvider.request(PermissionType.STORAGE_MANAGER)
            }
        } else {
            viewModel.download(update)
        }
    }

    private fun cancelSingle(update: Update) {
        viewModel.cancelDownload(update.packageName)
    }

    private fun cancelAll() {
        viewModel.cancelAll()
    }
}
