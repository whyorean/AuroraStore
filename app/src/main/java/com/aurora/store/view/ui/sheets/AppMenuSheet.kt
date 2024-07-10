/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *  Copyright (C) 2022, The Calyx Institute
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
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.aurora.extensions.openInfo
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.databinding.SheetAppMenuBinding
import com.aurora.store.util.PackageUtil
import com.aurora.store.viewmodel.sheets.SheetsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus

@AndroidEntryPoint
class AppMenuSheet : BottomSheetDialogFragment(R.layout.sheet_app_menu) {

    companion object {
        const val TAG = "APP_MENU_SHEET"
    }

    private var _binding: SheetAppMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SheetsViewModel by viewModels()
    private val args: AppMenuSheetArgs by navArgs()

    private val exportMimeType = "application/zip"

    private val requestDocumentCreation =
        registerForActivityResult(ActivityResultContracts.CreateDocument(exportMimeType)) {
            if (it != null) {
                viewModel.copyInstalledApp(requireContext(), args.app, it)
            } else {
                toast(R.string.failed_apk_export)
            }
            dismissAllowingStateLoss()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = SheetAppMenuBinding.bind(view)

        val blacklistProvider = BlacklistProvider.with(requireContext())
        val isBlacklisted: Boolean = blacklistProvider.isBlacklisted(args.app.packageName)

        with(binding.navigationView) {
            //Switch strings for Add/Remove Blacklist
            val blackListMenu: MenuItem = menu.findItem(R.id.action_blacklist)
            blackListMenu.setTitle(
                if (isBlacklisted)
                    R.string.action_whitelist
                else
                    R.string.action_blacklist_add
            )

            //Show/Hide actions based on installed status
            val installed = PackageUtil.isInstalled(requireContext(), args.app.packageName)
            menu.findItem(R.id.action_uninstall).isVisible = installed
            menu.findItem(R.id.action_local).isVisible = installed

            setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.action_blacklist -> {

                        if (isBlacklisted) {
                            blacklistProvider.whitelist(args.app.packageName)
                            requireContext().toast(R.string.toast_apk_whitelisted)
                        } else {
                            blacklistProvider.blacklist(args.app.packageName)
                            requireContext().toast(R.string.toast_apk_blacklisted)
                        }

                        dismissAllowingStateLoss()
                        EventBus.getDefault()
                            .post(BusEvent.Blacklisted(args.app.packageName, ""))
                    }

                    R.id.action_local -> {
                        requestDocumentCreation.launch("${args.app.packageName}.zip")
                    }

                    R.id.action_uninstall -> {
                        AppInstaller.uninstall(requireContext(), args.app.packageName)
                        dismissAllowingStateLoss()
                    }

                    R.id.action_info -> {
                        requireContext().openInfo(args.app.packageName)
                        dismissAllowingStateLoss()
                    }
                }
                false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
