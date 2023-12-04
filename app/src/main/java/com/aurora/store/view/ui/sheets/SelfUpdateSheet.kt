/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.aurora.store.view.ui.sheets

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aurora.Constants
import com.aurora.store.R
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.data.service.SelfUpdateService
import com.aurora.store.databinding.SheetSelfUpdateBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelfUpdateSheet : BaseBottomSheet() {

    private lateinit var B: SheetSelfUpdateBinding
    private lateinit var selfUpdate: SelfUpdate

    companion object {

        const val TAG = "ManualDownloadSheet"

        @JvmStatic
        fun newInstance(
            selfUpdate: SelfUpdate
        ): SelfUpdateSheet {
            return SelfUpdateSheet().apply {
                arguments = Bundle().apply {
                    putString(Constants.STRING_EXTRA, gson.toJson(selfUpdate))
                }
            }
        }
    }

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        B = SheetSelfUpdateBinding.inflate(inflater)
        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = arguments
        bundle?.let {
            val rawUpdate = bundle.getString(Constants.STRING_EXTRA, "{}")
            selfUpdate = gson.fromJson(rawUpdate, SelfUpdate::class.java)
            if (selfUpdate.versionName.isNotEmpty()) {
                inflateData()
                attachActions()
            } else {
                dismissAllowingStateLoss()
            }
        }
    }

    private fun inflateData() {
        B.txtLine2.text = ("${selfUpdate.versionName} (${selfUpdate.versionCode})")

        val messages: String = if (selfUpdate.changelog.isEmpty())
            getString(R.string.details_changelog_unavailable)
        else
            selfUpdate.changelog

        B.txtChangelog.text = messages.trim()
    }

    private fun attachActions() {
        B.btnPrimary.setOnClickListener {
            val intent = Intent(requireContext(), SelfUpdateService::class.java)
            intent.putExtra(Constants.STRING_EXTRA, gson.toJson(selfUpdate))
            requireContext().startService(intent)
            dismissAllowingStateLoss()
        }

        B.btnSecondary.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }
}
