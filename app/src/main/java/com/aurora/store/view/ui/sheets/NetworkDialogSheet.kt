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
import com.aurora.store.databinding.SheetNetworkBinding

class NetworkDialogSheet : BaseBottomSheet() {

    lateinit var B: SheetNetworkBinding

    private var type = 0

    companion object {
        @JvmStatic
        fun newInstance(type: Int): NetworkDialogSheet {
            return NetworkDialogSheet().apply {
                arguments = Bundle().apply {
                    putInt(Constants.INT_EXTRA, type)
                }
            }
        }
    }

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View? {
        B = SheetNetworkBinding.inflate(inflater, container, false)

        val bundle = arguments
        if (bundle != null) {
            type = bundle.getInt(Constants.INT_EXTRA, 0)
        }

        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {

    }
}