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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.Constants.GITLAB_URL
import com.aurora.extensions.browse
import com.aurora.store.R
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.room.download.Download
import com.aurora.store.databinding.FragmentDownloadBinding
import com.aurora.store.view.epoxy.views.DownloadViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadFragment : BaseFragment<FragmentDownloadBinding>() {

    @Inject
    lateinit var downloadHelper: DownloadHelper

    private lateinit var downloadList: List<Download>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.layoutToolbarAction.toolbar.apply {
            elevation = 0f
            title = getString(R.string.title_download_manager)
            navigationIcon = ContextCompat.getDrawable(view.context, R.drawable.ic_arrow_back)
            inflateMenu(R.menu.menu_download_main)
            setNavigationOnClickListener { findNavController().navigateUp() }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_force_clear_all -> {
                        viewLifecycleOwner.lifecycleScope.launch(NonCancellable) {
                            downloadHelper.clearAllDownloads()
                        }
                    }

                    R.id.action_cancel_all -> {
                        viewLifecycleOwner.lifecycleScope.launch(NonCancellable) {
                            downloadHelper.cancelAll()
                        }
                    }

                    R.id.action_clear_completed -> {
                        viewLifecycleOwner.lifecycleScope.launch(NonCancellable) {
                            downloadHelper.clearFinishedDownloads()
                        }
                    }
                }
                true
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            downloadHelper.downloadsList.collectLatest {
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
                        .message(getString(R.string.download_none))
                )
            } else {
                downloads.forEach {
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

    private fun openDownloadMenuSheet(packageName: String) {
        val download = downloadList.find { it.packageName == packageName }!!
        findNavController().navigate(
            DownloadFragmentDirections.actionDownloadFragmentToDownloadMenuSheet(download)
        )
    }
}
