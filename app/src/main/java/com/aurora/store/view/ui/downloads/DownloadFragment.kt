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
import android.text.format.DateUtils
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.Constants.GITLAB_URL
import com.aurora.extensions.browse
import com.aurora.store.R
import com.aurora.store.data.room.download.Download
import com.aurora.store.databinding.FragmentDownloadBinding
import com.aurora.store.view.epoxy.views.DownloadViewModel_
import com.aurora.store.view.epoxy.views.TextDividerViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.downloads.DownloadViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

@AndroidEntryPoint
class DownloadFragment : BaseFragment<FragmentDownloadBinding>() {

    private val viewModel: DownloadViewModel by viewModels()

    private lateinit var downloadList: List<Download>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.apply {
            setNavigationOnClickListener { findNavController().navigateUp() }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_force_clear_all -> {
                        viewLifecycleOwner.lifecycleScope.launch(NonCancellable) {
                            viewModel.downloadHelper.clearAllDownloads()
                        }
                    }

                    R.id.action_cancel_all -> {
                        viewLifecycleOwner.lifecycleScope.launch(NonCancellable) {
                            viewModel.downloadHelper.cancelAll()
                        }
                    }

                    R.id.action_clear_completed -> {
                        viewLifecycleOwner.lifecycleScope.launch(NonCancellable) {
                            viewModel.downloadHelper.clearFinishedDownloads()
                        }
                    }
                }
                true
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.downloadHelper.downloadsList.collectLatest {
                downloadList = it
                updateController(it.reversed())
            }
        }
    }

    private fun updateController(downloads: List<Download>) {
        binding.recycler.withModels {
            if (downloads.isEmpty()) {
                add(
                    NoAppViewModel_()
                        .id("no_downloads")
                        .message(R.string.download_none)
                )
            } else {
                downloads.groupBy {
                    DateUtils.getRelativeTimeSpanString(
                        it.downloadedAt,
                        Date().time,
                        DateUtils.DAY_IN_MILLIS
                    ).toString()
                }.forEach { (date, downloadList) ->
                    add(
                        TextDividerViewModel_()
                            .id(date)
                            .title(date)
                    )

                    downloadList.forEach {
                        add(
                            DownloadViewModel_()
                                .id(it.packageName)
                                .download(it)
                                .click { _ ->
                                    if (it.packageName == requireContext().packageName) {
                                        requireContext().browse(GITLAB_URL)
                                    } else {
                                        openDetailsFragment(it.packageName)
                                    }
                                }
                                .longClick { _ ->
                                    openDownloadMenuSheet(it.packageName)
                                    true
                                }
                        )
                    }
                }
            }
        }
    }

    private fun openDownloadMenuSheet(packageName: String) {
        val download = downloadList.find { it.packageName == packageName }!!
        findNavController().navigate(
            DownloadFragmentDirections.actionDownloadFragmentToDownloadMenuSheet(download)
        )
    }
}
