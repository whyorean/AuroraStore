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

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.aurora.store.R
import com.aurora.store.databinding.SheetBaseBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.lang.reflect.Modifier

abstract class BaseBottomSheet : BottomSheetDialogFragment() {

    lateinit var VM: SheetBaseBinding

    var gson: Gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
        .create()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(
            requireContext(),
            R.style.AppTheme_BottomSheet
        )

        VM = SheetBaseBinding.inflate(layoutInflater)

        bottomSheetDialog.setContentView(VM.root)

        val container = VM.container
        val contentView = onCreateContentView(
            LayoutInflater.from(requireContext()),
            container,
            savedInstanceState
        )

        if (contentView != null) {
            onContentViewCreated(contentView, savedInstanceState)
            container.addView(contentView)
        }

        bottomSheetDialog.setOnShowListener {
            val bottomSheet =
                bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null)
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
        }
        return bottomSheetDialog
    }

    abstract fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View?

    abstract fun onContentViewCreated(view: View, savedInstanceState: Bundle?)
}
