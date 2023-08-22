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

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import coil.transform.CircleCropTransformation
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.databinding.SheetDeviceMiuiBinding

class DeviceMiuiSheet : BaseBottomSheet() {

    private lateinit var B: SheetDeviceMiuiBinding

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        B = SheetDeviceMiuiBinding.inflate(inflater, container, false)

        inflateData()
        attachAction()

        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {

    }

    private fun inflateData() {
        B.imgIcon.load(R.drawable.ic_xiaomi_logo) {
            transformations(CircleCropTransformation())
        }
    }

    private fun attachAction() {
        B.btnPrimary.setOnClickListener {
            openDeveloperSettings()
        }

        B.btnSecondary.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun openDeveloperSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        } catch (e: Exception) {
            toast(R.string.toast_developer_setting_failed)
        }
    }
}
