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
import com.aurora.Constants
import com.aurora.extensions.copyToClipBoard
import com.aurora.extensions.load
import com.aurora.extensions.px
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.databinding.SheetInstallErrorBinding
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class InstallErrorDialogSheet : BaseBottomSheet() {

    private lateinit var B: SheetInstallErrorBinding

    private lateinit var app: App
    private lateinit var title: String
    private lateinit var error: String
    private lateinit var extra: String

    private var rawApp = String()

    companion object {
        private const val DIALOG_TITLE = "DIALOG_TITLE"
        private const val DIALOG_ERROR = "DIALOG_ERROR"
        private const val DIALOG_EXTRA = "DIALOG_EXTRA"

        @JvmStatic
        fun newInstance(
            app: App,
            title: String?,
            error: String?,
            extra: String?
        ): InstallErrorDialogSheet {
            return InstallErrorDialogSheet().apply {
                arguments = Bundle().apply {
                    putString(Constants.STRING_EXTRA, gson.toJson(app))
                    putString(DIALOG_TITLE, title)
                    putString(DIALOG_ERROR, error)
                    putString(DIALOG_EXTRA, extra)
                }
            }
        }
    }

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        B = SheetInstallErrorBinding.inflate(inflater, container, false)

        val bundle = arguments
        bundle?.let {
            rawApp = bundle.getString(Constants.STRING_EXTRA, "{}")
            app = gson.fromJson(rawApp, App::class.java)
            title = bundle.getString(DIALOG_TITLE, "")
            error = bundle.getString(DIALOG_ERROR, "")
            extra = bundle.getString(DIALOG_EXTRA, "")

            if (app.packageName.isNotEmpty()) {
                inflateData()
            } else {
                dismissAllowingStateLoss()
            }
        }

        attachAction()

        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {

    }

    private fun inflateData() {
        B.imgIcon.load(app.iconArtwork.url) {
            transform(CircleCrop())
        }

        B.txtLine1.text = app.displayName
        B.txtLine2.text = error
        B.txtLine3.text = extra
    }

    private fun attachAction() {
        B.btnPrimary.setOnClickListener {
            dismissAllowingStateLoss()
        }

        B.btnSecondary.setOnClickListener {
            if (::extra.isInitialized) {
                requireContext().copyToClipBoard(extra)
                requireContext().toast(R.string.toast_clipboard_copied)
            }
        }
    }
}
