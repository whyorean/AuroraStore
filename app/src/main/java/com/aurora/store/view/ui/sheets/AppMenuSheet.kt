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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.aurora.Constants
import com.aurora.extensions.isRAndAbove
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.databinding.SheetAppMenuBinding
import com.aurora.store.util.ApkCopier
import com.aurora.store.util.PackageUtil
import com.aurora.extensions.openInfo
import com.aurora.extensions.toast
import nl.komponents.kovenant.task
import org.greenrobot.eventbus.EventBus

class AppMenuSheet : BaseBottomSheet() {

    private lateinit var B: SheetAppMenuBinding
    private lateinit var app: App

    private val startForPermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                task { ApkCopier(requireContext(), app.packageName).copy() }
            } else {
                toast(R.string.permissions_denied)
            }
        }

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        B = SheetAppMenuBinding.inflate(layoutInflater)
        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments != null) {
            val bundle = arguments
            val stringExtra = bundle!!.getString(Constants.STRING_EXTRA)
            app = gson.fromJson(stringExtra, App::class.java)
            setupNavigationView()
        } else {
            dismissAllowingStateLoss()
        }
    }

    private fun setupNavigationView() {

        val blacklistProvider = BlacklistProvider.with(requireContext())
        val isBlacklisted: Boolean = blacklistProvider.isBlacklisted(app.packageName)

        with(B.navigationView) {
            //Switch strings for Add/Remove Blacklist
            val blackListMenu: MenuItem = menu.findItem(R.id.action_blacklist)
            blackListMenu.setTitle(
                if (isBlacklisted)
                    R.string.action_whitelist
                else
                    R.string.action_blacklist_add
            )

            //Show/Hide actions based on installed status
            val installed = PackageUtil.isInstalled(requireContext(), app.packageName)
            menu.findItem(R.id.action_uninstall).isVisible = installed
            menu.findItem(R.id.action_local).isVisible = installed

            setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.action_blacklist -> {

                        if (isBlacklisted) {
                            blacklistProvider.whitelist(app.packageName)
                            requireContext().toast(R.string.toast_apk_whitelisted)
                        } else {
                            blacklistProvider.blacklist(app.packageName)
                            requireContext().toast(R.string.toast_apk_blacklisted)
                        }

                        EventBus.getDefault()
                            .post(BusEvent.Blacklisted(app.packageName, ""))
                    }

                    R.id.action_local -> {
                        export()
                    }

                    R.id.action_uninstall -> {
                        task {
                            AppInstaller.getInstance(requireContext())
                                .getPreferredInstaller().uninstall(app.packageName)
                        }
                    }

                    R.id.action_info -> {
                        requireContext().openInfo(app.packageName)
                    }
                }
                dismissAllowingStateLoss()
                false
            }
        }
    }

    private fun export() {
        if (isRAndAbove()) {
            if (!Environment.isExternalStorageManager()) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            } else {
                task {
                    ApkCopier(requireContext(), app.packageName).copy()
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                task { ApkCopier(requireContext(), app.packageName).copy() }
            } else {
                startForPermissions.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    companion object {
        const val TAG = "APP_MENU_SHEET"
    }
}