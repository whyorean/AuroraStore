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

package com.aurora.store.view.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.aurora.extensions.copyToClipBoard
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.databinding.SheetDownloadMenuBinding
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class DownloadMenuSheet : BaseBottomSheet() {

    private lateinit var B: SheetDownloadMenuBinding
    private lateinit var fetch: Fetch

    private val args: DownloadMenuSheetArgs by navArgs()
    private var status by Delegates.notNull<Int>()

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        B = SheetDownloadMenuBinding.inflate(layoutInflater)
        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch = DownloadManager
            .with(requireContext())
            .getFetchInstance()

        status = args.downloadFile.download.status.value
        attachNavigation()
    }

    private fun attachNavigation() {
        with(B.navigationView) {
            if (status == Status.PAUSED.value || status == Status.COMPLETED.value || status == Status.CANCELLED.value) {
                menu.findItem(R.id.action_pause).isVisible = false
            }

            if (status == Status.DOWNLOADING.value || status == Status.COMPLETED.value || status == Status.QUEUED.value) {
                menu.findItem(R.id.action_resume).isVisible = false
            }

            if (status == Status.COMPLETED.value || status == Status.CANCELLED.value) {
                menu.findItem(R.id.action_cancel).isVisible = false
            }

            setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.action_copy -> {
                        requireContext().copyToClipBoard(args.downloadFile.download.url)
                        requireContext().toast(requireContext().getString(R.string.toast_clipboard_copied))
                    }
                    R.id.action_pause -> {
                        fetch.pause(args.downloadFile.download.id)
                    }
                    R.id.action_resume -> if (status == Status.FAILED.value || status == Status.CANCELLED.value) {
                        fetch.retry(args.downloadFile.download.id)
                    } else {
                        fetch.resume(args.downloadFile.download.id)
                    }
                    R.id.action_cancel -> {
                        fetch.cancel(args.downloadFile.download.id)
                    }
                    R.id.action_clear -> {
                        fetch.delete(args.downloadFile.download.id)
                    }
                }
                dismissAllowingStateLoss()
                false
            }
        }
    }
}
