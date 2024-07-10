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
import android.view.View
import androidx.navigation.fragment.navArgs
import coil.load
import coil.transform.CircleCropTransformation
import com.aurora.extensions.copyToClipBoard
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.databinding.SheetInstallErrorBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InstallErrorDialogSheet : BaseDialogSheet<SheetInstallErrorBinding>() {

    private val args: InstallErrorDialogSheetArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgIcon.load(args.app.iconArtwork.url) {
            transformations(CircleCropTransformation())
        }

        binding.txtLine1.text = args.app.displayName
        binding.txtLine2.text = args.error
        binding.txtLine3.text = args.extra

        binding.btnPrimary.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.btnSecondary.setOnClickListener {
            requireContext().copyToClipBoard(args.extra)
            requireContext().toast(R.string.toast_clipboard_copied)
        }
    }
}
