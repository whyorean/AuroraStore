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

package com.aurora.store.view.ui.spoof

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.data.providers.NativeDeviceInfoProvider
import com.aurora.store.databinding.FragmentGenericWithPagerBinding
import com.aurora.store.util.PathUtil
import com.aurora.store.view.ui.commons.BaseFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SpoofFragment : BaseFragment<FragmentGenericWithPagerBinding>() {
    private val TAG = SpoofFragment::class.java.simpleName

    // Android is weird, even if export device config with proper mime type, it will refuse to open
    // it again with same mime type
    private val importMimeType = "application/octet-stream"
    private val exportMimeType = "text/x-java-properties"

    private val startForDocumentImport =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it != null) importDeviceConfig(it) else toast(R.string.toast_import_failed)
        }
    private val startForDocumentExport =
        registerForActivityResult(ActivityResultContracts.CreateDocument(exportMimeType)) {
            if (it != null) exportDeviceConfig(it) else toast(R.string.toast_export_failed)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.layoutActionToolbar.toolbar.apply {
            elevation = 0f
            title = getString(R.string.title_spoof_manager)
            navigationIcon = ContextCompat.getDrawable(view.context, R.drawable.ic_arrow_back)
            inflateMenu(R.menu.menu_import_export)
            setNavigationOnClickListener { findNavController().navigateUp() }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_import -> {
                        startForDocumentImport.launch(arrayOf(importMimeType))
                    }

                    R.id.action_export -> {
                        startForDocumentExport
                            .launch("aurora_store_${Build.BRAND}_${Build.DEVICE}.properties")
                    }
                }
                true
            }
        }

        // ViewPager
        binding.pager.adapter = ViewPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)

        TabLayoutMediator(
            binding.tabLayout,
            binding.pager,
            true
        ) { tab: TabLayout.Tab, position: Int ->
            when (position) {
                0 -> tab.text = getString(R.string.title_device)
                1 -> tab.text = getString(R.string.title_language)
                else -> {
                }
            }
        }.attach()
    }

    override fun onDestroyView() {
        binding.pager.adapter = null
        super.onDestroyView()
    }

    private fun importDeviceConfig(uri: Uri) {
        try {
            context?.contentResolver?.openInputStream(uri)?.use { input ->
                PathUtil.getNewEmptySpoofConfig(requireContext()).outputStream().use {
                    input.copyTo(it)
                }
            }
            toast(R.string.toast_import_success)
            activity?.recreate()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to import device config", exception)
            toast(R.string.toast_import_failed)
        }
    }

    private fun exportDeviceConfig(uri: Uri) {
        try {
            NativeDeviceInfoProvider(requireContext())
                .getNativeDeviceProperties()
                .store(context?.contentResolver?.openOutputStream(uri), "DEVICE_CONFIG")
            toast(R.string.toast_export_success)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to export device config", exception)
            toast(R.string.toast_export_failed)
        }
    }

    internal class ViewPagerAdapter(fragment: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragment, lifecycle) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DeviceSpoofFragment.newInstance()
                1 -> LocaleSpoofFragment.newInstance()
                else -> Fragment()
            }
        }

        override fun getItemCount(): Int {
            return 2
        }
    }
}
