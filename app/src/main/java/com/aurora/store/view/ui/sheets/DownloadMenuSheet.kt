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
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.aurora.extensions.copyToClipBoard
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.databinding.SheetDownloadMenuBinding
import com.aurora.store.util.DownloadWorkerUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadMenuSheet : BaseBottomSheet() {

    private var _binding: SheetDownloadMenuBinding? = null
    private val binding get() = _binding!!

    private val args: DownloadMenuSheetArgs by navArgs()
    private val playStoreURL = "https://play.google.com/store/apps/details?id="

    @Inject
    lateinit var downloadWorkerUtil: DownloadWorkerUtil

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetDownloadMenuBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.navigationView) {
            menu.findItem(R.id.action_cancel).isVisible = !args.download.isFinished
            menu.findItem(R.id.action_clear).isVisible = args.download.isFinished

            setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.action_copy -> {
                        requireContext().copyToClipBoard(
                            "${playStoreURL}${args.download.packageName}"
                        )
                        requireContext().toast(requireContext().getString(R.string.toast_clipboard_copied))
                    }
                    R.id.action_cancel -> {
                        findViewTreeLifecycleOwner()?.lifecycleScope?.launch(NonCancellable) {
                            downloadWorkerUtil.cancelDownload(args.download.packageName)
                        }
                    }
                    R.id.action_clear -> {
                        findViewTreeLifecycleOwner()?.lifecycleScope?.launch(NonCancellable) {
                            downloadWorkerUtil.clearDownload(
                                args.download.packageName,
                                args.download.versionCode
                            )
                        }
                    }
                }
                dismissAllowingStateLoss()
                false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
