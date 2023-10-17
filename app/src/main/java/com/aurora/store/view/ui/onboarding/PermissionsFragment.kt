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
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.isTAndAbove
import com.aurora.extensions.toast
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.model.Permission
import com.aurora.store.databinding.FragmentOnboardingPermissionsBinding
import com.aurora.store.util.isExternalStorageAccessible
import com.aurora.store.view.epoxy.views.preference.PermissionViewModel_
import com.aurora.store.view.ui.commons.BaseFragment


class PermissionsFragment : BaseFragment(R.layout.fragment_onboarding_permissions) {

    private var _binding: FragmentOnboardingPermissionsBinding? = null
    private val binding get() = _binding!!

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
        _binding = FragmentOnboardingPermissionsBinding.bind(view)

        // RecyclerView
        val installerList = mutableListOf(
            Permission(
                2,
                getString(R.string.onboarding_permission_installer),
                getString(R.string.onboarding_permission_installer_desc)
            )
        )

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
            val writeExternalStorage =
                if (!isRAndAbove()) isExternalStorageAccessible(requireContext()) else true
            val postNotifications = if (isTAndAbove()) ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED else true
            val storageManager = if (isRAndAbove()) Environment.isExternalStorageManager() else true
            val canInstallPackages = if (isOAndAbove()) {
                requireContext().packageManager.canRequestPackageInstalls()
            } else {
                true
            }

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
                                2 -> canInstallPackages
                                3 -> postNotifications
                                else -> false
                            }
                        )
                        .click { _ ->
                            when (it.id) {
                                0 -> checkStorageAccessPermission()
                                1 -> requestStorageManagerPermission()
                                2 -> requestPackageManagerPermission()
                                3 -> checkPostNotificationsPermission()
                            }
                        }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkStorageAccessPermission() {
        startForPermissions.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun checkPostNotificationsPermission() {
        if (isTAndAbove()) {
            startForPermissions.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestStorageManagerPermission() {
        if (isRAndAbove()) {
            startForStorageManagerResult.launch(
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            )
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