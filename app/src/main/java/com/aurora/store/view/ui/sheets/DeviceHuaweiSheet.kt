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

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aurora.extensions.load
import com.aurora.extensions.toast
import com.aurora.gplayapi.DeviceManager
import com.aurora.store.R
import com.aurora.store.data.providers.SpoofProvider
import com.aurora.store.databinding.SheetDeviceHuaweiBinding
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import java.util.*

class DeviceHuaweiSheet : BaseBottomSheet() {

    private lateinit var B: SheetDeviceHuaweiBinding

    companion object {

        const val TAG = "DeviceHuaweiSheet"

        @JvmStatic
        fun newInstance(): DeviceHuaweiSheet {
            return DeviceHuaweiSheet().apply {
                arguments = Bundle().apply {

                }
            }
        }
    }

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        B = SheetDeviceHuaweiBinding.inflate(inflater, container, false)

        inflateData()
        attachAction()

        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {

    }

    private fun inflateData() {
        B.imgIcon.load(R.drawable.ic_huawei_logo) {
            transform(CircleCrop())
        }
    }

    private fun attachAction() {
        B.btnPrimary.setOnClickListener {
            applySpoof()
        }

        B.btnSecondary.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun applySpoof() {
        val properties: Properties? = when (Build.VERSION.SDK_INT) {
            30, 29 -> DeviceManager.loadProperties("op_8_pro.properties")
            28 -> DeviceManager.loadProperties("nk_9.properties")
            27 -> DeviceManager.loadProperties("mi_8_se.properties")
            26 -> DeviceManager.loadProperties("op_3.properties")
            else -> DeviceManager.loadProperties("op_x.properties")
        }

        properties?.let {
            SpoofProvider(requireContext()).setSpoofDeviceProperties(it)
            toast(R.string.toast_spoof_applied)
            dismissAllowingStateLoss()
        }
    }
}
