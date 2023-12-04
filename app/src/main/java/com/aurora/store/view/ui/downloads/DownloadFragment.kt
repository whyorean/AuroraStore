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

package com.aurora.store.view.ui.downloads

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.aurora.store.MobileNavigationDirections
import com.aurora.store.R
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.data.model.DownloadFile
import com.aurora.store.databinding.FragmentDownloadBinding
import com.aurora.store.util.Preferences
import com.aurora.store.view.epoxy.views.DownloadViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.tonyodev.fetch2.AbstractFetchListener
import com.tonyodev.fetch2.BuildConfig
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DownloadFragment : BaseFragment(R.layout.fragment_download) {

    private var _binding: FragmentDownloadBinding? = null
    private val binding: FragmentDownloadBinding
        get() = _binding!!

    private lateinit var fetch: Fetch

    private var fetchListener: FetchListener = object : AbstractFetchListener() {
        override fun onAdded(download: Download) {
            updateDownloadsList()
        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            updateDownloadsList()
        }

        override fun onCompleted(download: Download) {
            updateDownloadsList()
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            updateDownloadsList()
        }

        override fun onProgress(
            download: Download,
            etaInMilliSeconds: Long,
            downloadedBytesPerSecond: Long
        ) {
            updateDownloadsList()
        }

        override fun onPaused(download: Download) {
            updateDownloadsList()
        }

        override fun onResumed(download: Download) {
            updateDownloadsList()
        }

        override fun onCancelled(download: Download) {
            updateDownloadsList()
        }

        override fun onRemoved(download: Download) {
            updateDownloadsList()
        }

        override fun onDeleted(download: Download) {
            updateDownloadsList()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentDownloadBinding.bind(view)

        // Toolbar
        binding.layoutToolbarAction.toolbar.apply {
            elevation = 0f
            title = getString(R.string.title_download_manager)
            navigationIcon = ContextCompat.getDrawable(view.context, R.drawable.ic_arrow_back)
            inflateMenu(R.menu.menu_download_main)
            setNavigationOnClickListener { findNavController().navigateUp() }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_pause_all -> {
                        fetch.pauseAll()
                    }

                    R.id.action_resume_all -> {
                        fetch.resumeAll()
                    }

                    R.id.action_cancel_all -> {
                        fetch.cancelAll()
                    }

                    R.id.action_clear_completed -> {
                        fetch.removeAllWithStatus(Status.COMPLETED)
                        Preferences.getPrefs(view.context).edit()
                            .remove(Preferences.PREFERENCE_UNIQUE_GROUP_IDS).apply()
                    }

                    R.id.action_force_clear_all -> {
                        fetch.deleteAll()
                        Preferences.getPrefs(view.context).edit()
                            .remove(Preferences.PREFERENCE_UNIQUE_GROUP_IDS).apply()
                    }
                }
                true
            }
        }

        fetch = DownloadManager.with(view.context).fetch
        updateDownloadsList()

        binding.swipeRefreshLayout.setOnRefreshListener {
            updateDownloadsList()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::fetch.isInitialized)
            fetch.addListener(fetchListener)
    }

    override fun onPause() {
        if (::fetch.isInitialized)
            fetch.removeListener(fetchListener)
        super.onPause()
    }

    override fun onDestroyView() {
        if (::fetch.isInitialized) fetch.removeListener(fetchListener)
        super.onDestroyView()
        _binding = null
    }

    private fun updateDownloadsList() {
        if (::fetch.isInitialized)
            fetch.getDownloads { downloads ->
                updateController(
                    downloads
                        .filter { it.id != BuildConfig.APPLICATION_ID.hashCode() }
                        .sortedWith { o1, o2 -> o2.created.compareTo(o1.created) }
                        .map { DownloadFile(it) }
                )
            }
    }

    private fun updateController(downloads: List<DownloadFile>) {
        binding.recycler.withModels {
            if (downloads.isEmpty()) {
                add(
                    NoAppViewModel_()
                        .id("no_downloads")
                        .message(getString(R.string.download_none))
                )
            } else {
                downloads.forEach {
                    add(
                        DownloadViewModel_()
                            .id(it.download.id, it.download.progress, it.download.status.value)
                            .download(it)
                            .click { _ -> openDetailsActivity(it) }
                            .longClick { _ ->
                                openDownloadMenuSheet(it)
                                false
                            }
                    )
                }
            }
        }
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun openDetailsActivity(downloadFile: DownloadFile) {
        findNavController().navigate(
            MobileNavigationDirections.actionGlobalAppDetailsFragment(downloadFile.download.tag!!)
        )
    }

    private fun openDownloadMenuSheet(downloadFile: DownloadFile) {
        findNavController().navigate(
            DownloadFragmentDirections.actionDownloadFragmentToDownloadMenuSheet(downloadFile)
        )
    }
}
