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

package com.aurora.store.view.ui.onboarding

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.aurora.extensions.isMAndAbove
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.isTAndAbove
import com.aurora.extensions.toast
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.model.Permission
import com.aurora.store.databinding.FragmentOnboardingPermissionsBinding
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.isExternalStorageAccessible
import com.aurora.store.view.epoxy.views.preference.PermissionViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionsFragment : BaseFragment<FragmentOnboardingPermissionsBinding>() {

    private val startForDozeResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            context?.let {
                val powerManager = it.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (isMAndAbove() && powerManager.isIgnoringBatteryOptimizations(it.packageName)) {
                    toast(R.string.toast_permission_granted)
                    binding.epoxyRecycler.requestModelBuild()
                }
            }
        }
    private val startForPackageManagerResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isOAndAbove() && requireContext().packageManager.canRequestPackageInstalls()) {
                toast(R.string.toast_permission_granted)
                binding.epoxyRecycler.requestModelBuild()
            }
        }
    private val startForStorageManagerResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isRAndAbove() && Environment.isExternalStorageManager()) {
                toast(R.string.toast_permission_granted)
                binding.epoxyRecycler.requestModelBuild()
            }
        }
    private val startForPermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                toast(R.string.toast_permission_granted)
                binding.epoxyRecycler.requestModelBuild()
            } else {
                toast(R.string.permissions_denied)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView
        val installerList = mutableListOf(
            Permission(
                2,
                getString(R.string.onboarding_permission_installer),
                getString(R.string.onboarding_permission_installer_desc)
            )
        )

        if (isMAndAbove()) {
            installerList.add(
                Permission(
                    4,
                    getString(R.string.onboarding_permission_doze),
                    getString(R.string.onboarding_permission_doze_desc)
                )
            )
        }

        if (isRAndAbove()) {
            installerList.add(
                Permission(
                    1,
                    getString(R.string.onboarding_permission_esm),
                    getString(R.string.onboarding_permission_esa_desc)
                )
            )
        } else {
            installerList.add(
                Permission(
                    0,
                    getString(R.string.onboarding_permission_esa),
                    getString(R.string.onboarding_permission_esa_desc)
                )
            )
        }

        if (isTAndAbove()) {
            installerList.add(
                Permission(
                    3,
                    getString(R.string.onboarding_permission_notifications),
                    getString(R.string.onboarding_permission_notifications_desc)
                )
            )
        }

        binding.epoxyRecycler.withModels {
            val dozeDisabled = if (isMAndAbove()) {
                val powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
            } else {
                true
            }

            val writeExternalStorage =
                if (!isRAndAbove()) isExternalStorageAccessible(requireContext()) else true

            val postNotifications = if (isTAndAbove()) ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED else true

            val storageManager = if (isRAndAbove()) Environment.isExternalStorageManager() else true

            setFilterDuplicates(true)
            installerList.forEach {
                add(
                    PermissionViewModel_()
                        .id(it.id)
                        .permission(it)
                        .isGranted(
                            when (it.id) {
                                0 -> writeExternalStorage
                                1 -> storageManager
                                2 -> canInstallPackages()
                                3 -> postNotifications
                                4 -> dozeDisabled
                                else -> false
                            }
                        )
                        .click { _ ->
                            when (it.id) {
                                0 -> checkStorageAccessPermission()
                                1 -> requestStorageManagerPermission()
                                2 -> requestPackageManagerPermission()
                                3 -> checkPostNotificationsPermission()
                                4 -> requestDozePermission()
                            }
                        }
                )
            }
        }
    }

    private fun canInstallPackages(): Boolean {
        if (isOAndAbove()) {
            return requireContext().packageManager.canRequestPackageInstalls()
        } else {
            return true
        }
    }

    private fun requestDozePermission() {
        if (isMAndAbove()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            startForDozeResult.launch(intent)
        }
    }

    private fun checkStorageAccessPermission() {
        if (canInstallPackages()) {
            startForPermissions.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            toast(R.string.toast_permission_installer_required)
        }
    }

    private fun checkPostNotificationsPermission() {
        if (isTAndAbove()) {
            startForPermissions.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestStorageManagerPermission() {
        if (isRAndAbove()) {
            if (canInstallPackages()) {
                try {
                    startForStorageManagerResult.launch(PackageUtil.getStorageManagerIntent())
                } catch (_: ActivityNotFoundException) {
                    startForStorageManagerResult.launch(PackageUtil.getStorageManagerIntent(true))
                }
            } else {
                toast(R.string.toast_permission_installer_required)
            }
        }
    }

    private fun requestPackageManagerPermission() {
        if (isOAndAbove()) {
            startForPackageManagerResult.launch(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                )
            )
        }
    }

}
