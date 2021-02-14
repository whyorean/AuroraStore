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
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.R
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.downloader.RequestBuilder
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.FragmentUpdatesBinding
import com.aurora.store.util.Log
import com.aurora.store.util.extensions.flushAndAdd
import com.aurora.store.util.extensions.toast
import com.aurora.store.view.epoxy.views.AppUpdateViewModel_
import com.aurora.store.view.epoxy.views.UpdateHeaderViewModel_
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

    private val appList: MutableList<App> = mutableListOf()

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
        purchaseHelper = PurchaseHelper.with(authData)

        fetch = DownloadManager.with(requireContext()).fetch
        fetchListener = object : AbstractFetchGroupListener() {
            override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                super.onCompleted(groupId, download, fetchGroup)
                if (fetchGroup.groupDownloadProgress == 100) {
                    install(download.tag!!, fetchGroup.downloads)
                }
            }
        }

        fetch.addListener(fetchListener)

        return B.root
    }

    override fun onDestroyView() {
        if (::fetch.isInitialized && ::fetchListener.isInitialized) {
            fetch.removeListener(fetchListener)
        }
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VM.liveData.observe(viewLifecycleOwner, {
            appList.flushAndAdd(it)
            updateController(it)
            B.swipeRefreshLayout.isRefreshing = false
        })

        B.swipeRefreshLayout.setOnRefreshListener {
            VM.observe()
        }

        updateController(null)
    }

    private fun updateController(appList: List<App>?) {
        B.recycler.withModels {
            setFilterDuplicates(true)
            if (appList == null) {
                for (i in 1..6) {
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
                        if (VM.selectedUpdates.isEmpty()) {
                            UpdateHeaderViewModel_()
                                .id("header_all")
                                .title("${appList.size} updates available")
                                .action(getString(R.string.action_update_all))
                                .click { v ->
                                    updateAction(false)
                                }
                        } else {
                            UpdateHeaderViewModel_()
                                .id("header_selected")
                                .title("${VM.selectedUpdates.size} updates selected")
                                .action(getString(R.string.action_update))
                                .click { v ->
                                    updateAction(true)
                                }
                        }
                    )

                    appList.forEach { app ->
                        add(
                            AppUpdateViewModel_()
                                .id(app.id)
                                .app(app)
                                .click { _ -> openDetailsActivity(app) }
                                .longClick { _ ->
                                    openAppMenuSheet(app)
                                    false
                                }
                                .markChecked(VM.selectedUpdates.contains(app.packageName))
                                .checked { _, isChecked ->
                                    if (isChecked)
                                        VM.selectedUpdates.add(app.packageName)
                                    else
                                        VM.selectedUpdates.remove(app.packageName)

                                    requestModelBuild()
                                }
                        )
                    }
                }
            }
        }
    }

    private fun updateAction(selectedOnly: Boolean) {
        if (VM.isUpdating) {
            requireContext().toast("Update in progress, let previous batch finish first")
        } else {
            if (selectedOnly) {
                updateSelected()
            } else {
                updateAll()
            }
        }
    }

    private fun updateAll() {
        appList.forEach { app ->
            task {
                val files = purchaseHelper.purchase(app.packageName, app.versionCode, app.offerType)
                files.map { RequestBuilder.buildRequest(requireContext(), app, it) }
            } successUi {
                val requests = it.filter { request -> request.url.isNotEmpty() }.toList()
                if (requests.isNotEmpty()) {
                    VM.isUpdating = true
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
    }

    private fun updateSelected() {
        val selectedPackages = VM.selectedUpdates
        appList.forEach { app ->
            if (selectedPackages.contains(app.packageName)) {
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
                        VM.isUpdating = true
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
        }
    }

    @Synchronized
    private fun install(packageName: String, files: List<Download>) {
        task {
            AppInstaller.with(requireContext())
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