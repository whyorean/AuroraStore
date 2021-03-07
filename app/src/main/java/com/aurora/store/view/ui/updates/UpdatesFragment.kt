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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.aurora.Constants
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.R
import com.aurora.store.State
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.downloader.RequestBuilder
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.UpdateFile
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.FragmentUpdatesBinding
import com.aurora.store.util.Log
import com.aurora.store.view.epoxy.views.UpdateHeaderViewModel_
import com.aurora.store.view.epoxy.views.app.AppUpdateViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.view.ui.sheets.AppMenuSheet
import com.aurora.store.viewmodel.all.UpdatesViewModel
import com.tonyodev.fetch2.AbstractFetchGroupListener
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchGroup
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

class UpdatesFragment : BaseFragment() {

    private lateinit var B: FragmentUpdatesBinding
    private lateinit var VM: UpdatesViewModel

    private lateinit var authData: AuthData
    private lateinit var purchaseHelper: PurchaseHelper

    private lateinit var fetch: Fetch
    private lateinit var fetchListener: AbstractFetchGroupListener

    private var updateFileMap: MutableMap<Int, UpdateFile> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        B = FragmentUpdatesBinding.bind(
            inflater.inflate(
                R.layout.fragment_updates,
                container,
                false
            )
        )

        VM = ViewModelProvider(requireActivity()).get(UpdatesViewModel::class.java)

        authData = AuthProvider.with(requireContext()).getAuthData()
        purchaseHelper = PurchaseHelper(authData)

        fetch = DownloadManager.with(requireContext()).fetch
        fetchListener = object : AbstractFetchGroupListener() {

            override fun onAdded(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                VM.updateDownload(groupId, fetchGroup)
            }

            override fun onProgress(
                groupId: Int,
                download: Download,
                etaInMilliSeconds: Long,
                downloadedBytesPerSecond: Long,
                fetchGroup: FetchGroup
            ) {
                VM.updateDownload(groupId, fetchGroup)
            }

            override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (fetchGroup.groupDownloadProgress == 100) {
                    VM.updateDownload(groupId, fetchGroup)
                    install(download.tag!!, fetchGroup.downloads)
                }
            }

            override fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                VM.updateDownload(groupId, fetchGroup,true)
            }

            override fun onDeleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                VM.updateDownload(groupId, fetchGroup,true)
            }
        }

        return B.root
    }

    override fun onResume() {
        super.onResume()
        if (::fetch.isInitialized && ::fetchListener.isInitialized) {
            fetch.addListener(fetchListener)
        }
    }

    override fun onPause() {
        if (::fetch.isInitialized && ::fetchListener.isInitialized) {
            fetch.removeListener(fetchListener)
        }
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        VM.liveUpdateData.observe(viewLifecycleOwner, {
            updateFileMap = it
            updateController(updateFileMap)
            B.swipeRefreshLayout.isRefreshing = false
        })

        B.swipeRefreshLayout.setOnRefreshListener {
            VM.observe()
        }

        updateController(null)
    }

    private fun updateController(updateFileMap: MutableMap<Int, UpdateFile>?) {
        B.recycler.withModels {
            setFilterDuplicates(true)
            if (updateFileMap == null) {
                for (i in 1..6) {
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
                            .title("${updateFileMap.size} updates available")
                            .action(getString(R.string.action_update_all))
                            .click { _ -> updateAll() }
                    )

                    updateFileMap.values.forEach { updateFile ->
                        add(
                            AppUpdateViewModel_()
                                .id(updateFile.hashCode())
                                .updateFile(updateFile)
                                .click { _ -> openDetailsActivity(updateFile.app) }
                                .longClick { _ ->
                                    openAppMenuSheet(updateFile.app)
                                    false
                                }
                                .positiveAction { _ -> updateSingle(updateFile.app) }
                                .negativeAction { _ -> cancelSingle(updateFile.app) }
                                .state(updateFile.state)
                        )
                    }
                }
            }
        }
    }

    private fun updateSingle(app: App) {

        VM.updateState(app.id, State.QUEUED)

        task {
            val files = purchaseHelper.purchase(
                app.packageName,
                app.versionCode,
                app.offerType
            )

            files.map { RequestBuilder.buildRequest(requireContext(), app, it) }
        } successUi {
            val requests = it.filter { request -> request.url.isNotEmpty() }.toList()
            if (requests.isNotEmpty()) {
                fetch.enqueue(requests) {
                    Log.i("Updating ${app.displayName}")
                }
            } else {
                requireContext().toast("Failed to update ${app.displayName}")
            }
        } failUi {
            Log.e("Failed to update ${app.displayName}")
        }
    }

    private fun cancelSingle(app: App) {
        fetch.cancelGroup(app.id)
    }

    private fun updateAll() {
        updateFileMap.values.forEach { updateSingle(it.app) }
    }

    @Synchronized
    private fun install(packageName: String, files: List<Download>) {
        task {
            AppInstaller(requireContext())
                .getPreferredInstaller()
                .install(
                    packageName,
                    files
                        .filter { it.file.endsWith(".apk") }
                        .map { it.file }.toList()
                )
        }
    }

    private fun openAppMenuSheet(app: App) {
        val fragment = childFragmentManager.findFragmentByTag(AppMenuSheet.TAG)
        if (fragment != null)
            childFragmentManager.beginTransaction().remove(fragment)

        AppMenuSheet().apply {
            arguments = Bundle().apply {
                putString(Constants.STRING_EXTRA, gson.toJson(app))
            }
        }.show(
            childFragmentManager,
            AppMenuSheet.TAG
        )
    }
}