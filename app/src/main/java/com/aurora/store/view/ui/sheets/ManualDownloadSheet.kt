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

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import coil.load
import coil.transform.RoundedCornersTransformation
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.databinding.SheetManualDownloadBinding
import com.aurora.store.viewmodel.sheets.SheetsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManualDownloadSheet : BaseBottomSheet() {

    private lateinit var B: SheetManualDownloadBinding

    private val viewModel: SheetsViewModel by viewModels()
    private val args: ManualDownloadSheetArgs by navArgs()

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        B = SheetManualDownloadBinding.inflate(inflater)
        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isCancelable = false

        inflateData()
        attachActions()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        this.lifecycleScope.launch {
            viewModel.purchaseStatus.collectLatest {
                if (it) {
                    toast(R.string.toast_manual_available)
                    dismissAllowingStateLoss()
                } else {
                    toast(R.string.toast_manual_unavailable)
                }
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun inflateData() {
        B.imgIcon.load(args.app.iconArtwork.url) {
            placeholder(R.drawable.bg_placeholder)
            transformations(RoundedCornersTransformation(32F))
        }

        B.txtLine1.text = args.app.displayName
        B.txtLine2.text = args.app.packageName
        B.txtLine3.text = ("${args.app.versionName} (${args.app.versionCode})")

        B.versionCodeLayout.hint = "${args.app.versionCode}"
        B.versionCodeLayout.editText?.setText("${args.app.versionCode}")
    }

    private fun attachActions() {
        B.btnPrimary.setOnClickListener {
            val customVersionString = (B.versionCodeInp.text).toString()
            if (customVersionString.isEmpty())
                B.versionCodeInp.error = "Enter version code"
            else {
                viewModel.purchase(requireContext(), args.app, customVersionString.toInt())
            }
        }

        B.btnSecondary.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }
}
