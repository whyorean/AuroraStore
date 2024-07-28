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

import android.os.Bundle
import android.view.View
import com.aurora.extensions.isMAndAbove
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.extensions.isTAndAbove
import com.aurora.store.PermissionType
import com.aurora.store.R
import com.aurora.store.data.model.Permission
import com.aurora.store.data.providers.PermissionProvider
import com.aurora.store.databinding.FragmentOnboardingPermissionsBinding
import com.aurora.store.view.epoxy.views.TextDividerViewModel_
import com.aurora.store.view.epoxy.views.preference.PermissionViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionsFragment : BaseFragment<FragmentOnboardingPermissionsBinding>() {
    private lateinit var permissionProvider: PermissionProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionProvider = PermissionProvider(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateController()
    }

    override fun onResume() {
        super.onResume()
        updateController()
    }

    override fun onDestroy() {
        permissionProvider.unregister()
        super.onDestroy()
    }

    private fun permissionList(): List<Permission> {
        val permissions = mutableListOf(
            Permission(
                PermissionType.INSTALL_UNKNOWN_APPS,
                getString(R.string.onboarding_permission_installer),
                if (isOAndAbove()) {
                    getString(R.string.onboarding_permission_installer_desc)
                } else {
                    getString(R.string.onboarding_permission_installer_legacy_desc)
                }
            )
        )

        if (isRAndAbove()) {
            permissions.add(
                Permission(
                    PermissionType.STORAGE_MANAGER,
                    getString(R.string.onboarding_permission_esm),
                    getString(R.string.onboarding_permission_esa_desc),
                    false
                )
            )
        } else {
            permissions.add(
                Permission(
                    PermissionType.EXTERNAL_STORAGE,
                    getString(R.string.onboarding_permission_esa),
                    getString(R.string.onboarding_permission_esa_desc),
                    false
                )
            )
        }

        if (isMAndAbove()) {
            permissions.add(
                Permission(
                    PermissionType.DOZE_WHITELIST,
                    getString(R.string.onboarding_permission_doze),
                    getString(R.string.onboarding_permission_doze_desc),
                    true
                )
            )
        }

        if (isTAndAbove()) {
            permissions.add(
                Permission(
                    PermissionType.POST_NOTIFICATIONS,
                    getString(R.string.onboarding_permission_notifications),
                    getString(R.string.onboarding_permission_notifications_desc),
                    true
                )
            )
        }

        if (isSAndAbove()) {
            permissions.add(
                Permission(
                    PermissionType.APP_LINKS,
                    "App Links",
                    "Enable Aurora Store to open links from the Play Store",
                    optional = true
                ),
            )
        }

        return permissions
    }

    private fun updateController() {
        binding.epoxyRecycler.withModels {
            setFilterDuplicates(true)

            add(
                TextDividerViewModel_()
                    .id("required_divider")
                    .title(getString(R.string.item_required))
            )

            permissionList()
                .filterNot { it.optional }
                .forEach { add(renderPermissionView(it)) }

            val optionalPermissions = permissionList().filter { it.optional }
            if (optionalPermissions.isNotEmpty()) {
                add(
                    TextDividerViewModel_()
                        .id("optional_divider")
                        .title(getString(R.string.item_optional))
                )

                optionalPermissions.forEach { add(renderPermissionView(it)) }
            }
        }
    }

    private fun renderPermissionView(permission: Permission): PermissionViewModel_ {
        return PermissionViewModel_()
            .id(permission.type.name)
            .permission(permission)
            .isGranted(permissionProvider.isGranted(permission.type))
            .click { _ -> permissionProvider.request(permission.type) }
    }
}
