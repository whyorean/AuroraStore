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
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.databinding.SheetAppPeekBinding
import com.aurora.store.util.CommonUtil
import com.aurora.extensions.load
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class AppPeekDialogSheet : BaseBottomSheet() {

    lateinit var B: SheetAppPeekBinding
    lateinit var app: App

    private var rawApp = String()

    companion object {
        @JvmStatic
        fun newInstance(app: App): AppPeekDialogSheet {
            return AppPeekDialogSheet().apply {
                arguments = Bundle().apply {
                    putString(Constants.STRING_EXTRA, gson.toJson(app))
                }
            }
        }
    }

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View? {
        B = SheetAppPeekBinding.inflate(inflater, container, false)
        val bundle = arguments
        if (bundle != null) {
            rawApp = bundle.getString(Constants.STRING_EXTRA, "")
            if (rawApp.isNotEmpty()) {
                app = gson.fromJson(rawApp, App::class.java)
                if (app.packageName.isNotEmpty()) {
                    draw()
                } else {
                    dismissAllowingStateLoss()
                }
            } else {
                dismissAllowingStateLoss()
            }
        }

        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {

    }

    private fun draw() {
        B.txtLine1.text = app.displayName
        B.imgIcon.load(app.iconArtwork.url) {
            transform(RoundedCorners(25))
        }
        B.txtLine2.text = app.developerName
        B.txtLine3.text = String.format(
            requireContext().getString(R.string.app_list_rating),
            CommonUtil.addSiPrefix(app.size),
            app.labeledRating,
            if (app.isFree)
                "Free"
            else
                "Paid"
        )
    }
}
