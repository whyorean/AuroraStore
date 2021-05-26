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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aurora.Constants
import com.aurora.extensions.load
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.SheetManualDownloadBinding
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.greenrobot.eventbus.EventBus

class ManualDownloadSheet : BaseBottomSheet() {

    private lateinit var B: SheetManualDownloadBinding
    private lateinit var app: App
    private lateinit var authData: AuthData
    private lateinit var purchaseHelper: PurchaseHelper

    companion object {

        const val TAG = "ManualDownloadSheet"

        @JvmStatic
        fun newInstance(
            app: App
        ): ManualDownloadSheet {
            return ManualDownloadSheet().apply {
                arguments = Bundle().apply {
                    putString(Constants.STRING_APP, gson.toJson(app))
                }
            }
        }
    }

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        B = SheetManualDownloadBinding.inflate(inflater)
        authData = AuthProvider.with(requireContext()).getAuthData()
        purchaseHelper = PurchaseHelper(authData)
        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = arguments
        bundle?.let {
            val rawApp = bundle.getString(Constants.STRING_APP, "{}")
            app = gson.fromJson(rawApp, App::class.java)
            if (app.packageName.isNotEmpty()) {
                inflateData()
                attachActions()
            } else {
                dismissAllowingStateLoss()
            }
        }
    }

    private fun inflateData() {
        B.imgIcon.load(app.iconArtwork.url) {
            placeholder(R.drawable.bg_placeholder)
            transform(RoundedCorners(32))
        }

        B.txtLine1.text = app.displayName
        B.txtLine2.text = app.packageName
        B.txtLine3.text = ("${app.versionName} (${app.versionCode})")

        B.versionCodeLayout.hint = "${app.versionCode}"
        B.versionCodeLayout.editText?.setText("${app.versionCode}")
    }

    private fun attachActions() {
        B.btnPrimary.setOnClickListener {
            val customVersionString = (B.versionCodeInp.text).toString()
            if (customVersionString.isEmpty())
                B.versionCodeInp.error = "Enter version code"
            else {
                val customVersion = customVersionString.toInt()

                task {
                    purchaseHelper.purchase(app.packageName, customVersion, app.offerType)
                } successUi {
                    if (it.isNotEmpty()) {
                        toast(R.string.toast_manual_available)
                        EventBus.getDefault().post(BusEvent.ManualDownload(app.packageName, customVersion))
                        dismissAllowingStateLoss()
                    } else {
                        toast(R.string.toast_manual_unavailable)
                    }
                } failUi {
                    toast(R.string.toast_manual_unavailable)
                }
            }
        }

        B.btnSecondary.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }
}
