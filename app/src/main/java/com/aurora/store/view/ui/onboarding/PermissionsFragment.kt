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
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.toast
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.model.Permission
import com.aurora.store.databinding.FragmentOnboardingPermissionsBinding
import com.aurora.store.view.epoxy.views.preference.PermissionViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions


class PermissionsFragment : BaseFragment() {

    private lateinit var B: FragmentOnboardingPermissionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        B = FragmentOnboardingPermissionsBinding.bind(
            inflater.inflate(
                R.layout.fragment_onboarding_permissions,
                container,
                false
            )
        )

        return B.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateController()
    }

    private fun updateController() {

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

        B.epoxyRecycler.withModels {
            val writeExternalStorage = if (!isRAndAbove()) ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED else true
            val storageManager = if (isRAndAbove()) Environment.isExternalStorageManager() else true
            val canInstallPackages = if (isOAndAbove()) requireContext().packageManager.canRequestPackageInstalls() else true
            canGoForward = writeExternalStorage && storageManager && canInstallPackages
            if (canGoForwardInitial == null) {
                canGoForwardInitial = canGoForward
            }
            if (canGoForward && canGoForwardInitial == false) {
                if (activity is OnboardingActivity) {
                    (activity!! as OnboardingActivity).refreshButtonState()
                }
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
                                else -> false
                            }
                        )
                        .click { _ ->
                            when (it.id) {
                                0 -> checkStorageAccessPermission()
                                1 -> checkStorageManagerPermission()
                                2 -> checkUnknownResourceInstallation()
                            }
                        }
                )
            }
        }
    }

    private fun checkStorageAccessPermission() = runWithPermissions(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) {
        toast(R.string.toast_permission_granted)
        B.epoxyRecycler.requestModelBuild()
    }

    private fun checkStorageManagerPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                toast("Permission granted")
            } else {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                    99
                )
            }
        }
    }

    private fun checkUnknownResourceInstallation() {
        if (isOAndAbove()) {
            startActivityForResult(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                ),
                999
            )
        } else {
            toast("Permission granted")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            99, 999 -> {
                toast(R.string.toast_permission_granted)
                B.epoxyRecycler.requestModelBuild()
            }
        }
    }

    private var canGoForward = false
    private var canGoForwardInitial: Boolean? = null

    fun canGoForward(): Boolean {
        return canGoForward
    }
}