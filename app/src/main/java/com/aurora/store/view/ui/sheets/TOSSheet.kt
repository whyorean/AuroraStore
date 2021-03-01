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
import com.aurora.extensions.browse
import com.aurora.store.databinding.SheetTosBinding
import com.aurora.store.util.Preferences

class TOSSheet : BaseBottomSheet() {

    private lateinit var B: SheetTosBinding

    companion object {

        const val TAG = "TOSSheet"

        @JvmStatic
        fun newInstance(): TOSSheet {
            return TOSSheet().apply {
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
        B = SheetTosBinding.inflate(inflater, container, false)

        attachAction()

        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {

    }

    private fun attachAction() {
        B.btnPrimary.setOnClickListener {
            requireContext().browse(Constants.TOS_URL)
            Preferences.putBoolean(requireContext(), Preferences.PREFERENCE_TOS_READ, true)
        }

        B.btnSecondary.setOnClickListener {
            Preferences.putBoolean(requireContext(), Preferences.PREFERENCE_TOS_READ, true)
            dismissAllowingStateLoss()
        }
    }
}
