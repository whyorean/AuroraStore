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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import com.aurora.extensions.isQAndAbove
import com.aurora.store.databinding.SheetNetworkBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NetworkDialogSheet : BaseDialogSheet<SheetNetworkBinding>() {

    private val TAG = NetworkDialogSheet::class.java.simpleName

    companion object {

        const val TAG = "NetworkDialogSheet"

        @JvmStatic
        fun newInstance(): NetworkDialogSheet {
            return NetworkDialogSheet().apply {

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnAction.setOnClickListener {
            if (isQAndAbove()) {
                startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
            } else {
                try {
                    startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                } catch (exception: ActivityNotFoundException) {
                    Log.i(TAG, "Unable to launch wireless settings")
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }
}
