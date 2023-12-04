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
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.State
import com.aurora.store.data.downloader.getGroupId
import com.aurora.store.data.model.UpdateFile
import com.aurora.store.data.service.UpdateService
import com.aurora.store.databinding.FragmentUpdatesBinding
import com.aurora.store.util.PathUtil
import com.aurora.store.util.isExternalStorageEnable
import com.aurora.store.view.epoxy.views.UpdateHeaderViewModel_
import com.aurora.store.view.epoxy.views.app.AppUpdateViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.all.UpdatesViewModel
import com.tonyodev.fetch2.AbstractFetchGroupListener
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchGroup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpdatesFragment : BaseFragment(R.layout.fragment_updates) {

    private lateinit var app: App

    private var _binding: FragmentUpdatesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UpdatesViewModel by viewModels()

    private val startForStorageManagerResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isRAndAbove() && Environment.isExternalStorageManager()) {
                updateSingle(app)
            } else {
                toast(R.string.permissions_denied)
            }
        }
    private val startForPermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { perm ->
            if (perm) updateSingle(app) else toast(R.string.permissions_denied)
        }

    val listOfActionsWhenServiceAttaches = ArrayList<Runnable>()
    private lateinit var fetchListener: AbstractFetchGroupListener

    private var updateService: UpdateService? = null
    private var attachToServiceCalled = false
    private var serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            updateService = (binder as UpdateService.UpdateServiceBinder).getUpdateService()
            updateService!!.registerFetchListener(fetchListener)
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
        }
    }

    private var updateFileMap: MutableMap<Int, UpdateFile> = mutableMapOf()

    override fun onResume() {
        getUpdateServiceInstance()
        super.onResume()
    }

    override fun onPause() {
        if (updateService != null) {
            updateService = null
            attachToServiceCalled = false
            requireContext().unbindService(serviceConnection)
        }
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (updateService != null) {
            updateService = null
            attachToServiceCalled = false
            requireContext().unbindService(serviceConnection)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUpdatesBinding.bind(view)

        fetchListener = object : AbstractFetchGroupListener() {

            override fun onAdded(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                viewModel.updateDownload(groupId, fetchGroup)
            }

            override fun onProgress(
                groupId: Int,
                download: Download,
                etaInMilliSeconds: Long,
                downloadedBytesPerSecond: Long,
                fetchGroup: FetchGroup
            ) {
                viewModel.updateDownload(groupId, fetchGroup)
            }

            override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (fetchGroup.groupDownloadProgress == 100) {
                    viewModel.updateDownload(groupId, fetchGroup, isComplete = true)
                }
            }

            override fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                viewModel.updateDownload(groupId, fetchGroup, isCancelled = true)
            }

            override fun onDeleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                viewModel.updateDownload(groupId, fetchGroup, isCancelled = true)
            }
        }

        getUpdateServiceInstance()

        viewModel.liveUpdateData.observe(viewLifecycleOwner) {
            updateFileMap = it
            updateController(updateFileMap)
            binding.swipeRefreshLayout.isRefreshing = false
            updateService?.liveUpdateData?.postValue(updateFileMap)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.observe()
        }

        updateController(null)
    }

    private fun updateController(updateFileMap: MutableMap<Int, UpdateFile>?) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            if (updateFileMap == null) {
                for (i in 1..10) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                if (updateFileMap.isEmpty()) {
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
                                "${updateFileMap.size} " +
                                        if (updateFileMap.size == 1)
                                            getString(R.string.update_available)
                                        else
                                            getString(R.string.updates_available)
                            )
                            .action(
                                if (viewModel.updateAllEnqueued)
                                    getString(R.string.action_cancel)
                                else
                                    getString(R.string.action_update_all)
                            )
                            .click { _ ->
                                if (viewModel.updateAllEnqueued)
                                    cancelAll()
                                else
                                    updateFileMap.values.forEach { updateSingle(it.app, true) }

                                requestModelBuild()
                            }
                    )

                    updateFileMap.values.forEach { updateFile ->
                        add(
                            AppUpdateViewModel_()
                                .id(updateFile.hashCode())
                                .updateFile(updateFile)
                                .click { _ ->
                                    openDetailsFragment(
                                        updateFile.app.packageName,
                                        updateFile.app
                                    )
                                }
                                .longClick { _ ->
                                    openAppMenuSheet(updateFile.app)
                                    false
                                }
                                .positiveAction { _ -> updateSingle(updateFile.app) }
                                .negativeAction { _ -> cancelSingle(updateFile.app) }
                                .installAction { _ ->
                                    updateFile.group?.downloads?.let {
                                        viewModel.install(
                                            requireContext(),
                                            updateFile.app.packageName,
                                            it
                                        )
                                    }
                                }
                                .state(updateFile.state)
                        )
                    }
                }
            }
        }
    }

    private fun runInService(runnable: Runnable) {
        if (updateService == null) {
            listOfActionsWhenServiceAttaches.add(runnable)
            getUpdateServiceInstance()
        } else {
            runnable.run()
        }
    }

    private fun updateSingle(app: App, updateAll: Boolean = false) {
        this.app = app
        runInService {
            viewModel.updateState(app.getGroupId(requireContext()), State.QUEUED)
            viewModel.updateAllEnqueued = updateAll

            if (PathUtil.needsStorageManagerPerm(app.fileList) ||
                requireContext().isExternalStorageEnable()
            ) {
                if (isRAndAbove()) {
                    if (!Environment.isExternalStorageManager()) {
                        startForStorageManagerResult.launch(
                            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        )
                    } else {
                        updateService?.updateApp(app)
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        updateService?.updateApp(app)
                    } else {
                        startForPermissions.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
            } else {
                updateService?.updateApp(app)
            }
        }
    }

    private fun cancelSingle(app: App) {
        runInService {
            updateService?.fetch?.cancelGroup(app.getGroupId(requireContext()))
        }
    }

    private fun cancelAll() {
        runInService {
            viewModel.updateAllEnqueued = false
            updateFileMap.values.forEach {
                updateService?.fetch?.cancelGroup(
                    it.app.getGroupId(
                        requireContext()
                    )
                )
            }
        }
    }

    private fun getUpdateServiceInstance() {
        if (updateService == null && !attachToServiceCalled) {
            attachToServiceCalled = true
            val intent = Intent(requireContext(), UpdateService::class.java)
            requireContext().startService(intent)
            requireContext().bindService(
                intent,
                serviceConnection,
                0
            )
        }
    }
}
