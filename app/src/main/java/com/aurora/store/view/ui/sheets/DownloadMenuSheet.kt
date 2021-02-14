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
import com.aurora.store.R
import com.aurora.store.data.downloader.DownloadManager
import com.aurora.store.databinding.SheetDownloadMenuBinding
import com.aurora.store.util.extensions.copyToClipBoard
import com.aurora.store.util.extensions.toast
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.Status

class DownloadMenuSheet : BaseBottomSheet() {

    private lateinit var B: SheetDownloadMenuBinding
    private lateinit var fetch: Fetch

    private var downloadId = 0
    private var status = 0
    private var url: String? = null


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

        if (arguments != null) {
            val bundle = arguments
            if (bundle != null) {
                downloadId = bundle.getInt(DOWNLOAD_ID)
                status = bundle.getInt(DOWNLOAD_STATUS)
                url = bundle.getString(DOWNLOAD_URL)
                attachNavigation()
            } else {
                dismissAllowingStateLoss()
            }
        } else {
            dismissAllowingStateLoss()
        }
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
                        requireContext().copyToClipBoard(url)
                        requireContext().toast(requireContext().getString(R.string.toast_clipboard_copied))
                    }
                    R.id.action_pause -> {
                        fetch.pause(downloadId)
                    }
                    R.id.action_resume -> if (status == Status.FAILED.value || status == Status.CANCELLED.value) {
                        fetch.retry(downloadId)
                    } else {
                        fetch.resume(downloadId)
                    }
                    R.id.action_cancel -> {
                        fetch.cancel(downloadId)
                    }
                    R.id.action_clear -> {
                        fetch.delete(downloadId)
                    }
                }
                dismissAllowingStateLoss()
                false
            }
        }
    }

    companion object {
        const val TAG = "DOWNLOAD_MENU_SHEET"
        const val DOWNLOAD_ID = "DOWNLOAD_ID"
        const val DOWNLOAD_STATUS = "DOWNLOAD_STATUS"
        const val DOWNLOAD_URL = "DOWNLOAD_URL"
    }
}