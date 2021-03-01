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

package com.aurora.store.view.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aurora.extensions.isMIUI
import com.aurora.extensions.isMiuiOptimizationDisabled
import com.aurora.extensions.showDialog
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.installer.ServiceInstaller
import com.aurora.store.data.model.Installer
import com.aurora.store.databinding.FragmentOnboardingInstallerBinding
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.aurora.store.util.save
import com.aurora.store.view.epoxy.views.preference.InstallerViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.view.ui.sheets.DeviceMiuiSheet
import com.google.gson.reflect.TypeToken
import com.topjohnwu.superuser.Shell
import java.nio.charset.StandardCharsets


class InstallerFragment : BaseFragment() {

    private lateinit var B: FragmentOnboardingInstallerBinding

    var installerId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        B = FragmentOnboardingInstallerBinding.bind(
            inflater.inflate(
                R.layout.fragment_onboarding_installer,
                container,
                false
            )
        )

        return B.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        installerId = Preferences.getInteger(
            requireContext(),
            PREFERENCE_INSTALLER_ID
        )

        val installerList = loadInstallersFromAssets()
        updateController(installerList)
    }

    private fun updateController(installerList: List<Installer>) {
        B.epoxyRecycler.withModels {
            setFilterDuplicates(true)
            installerList.forEach {
                add(
                    InstallerViewModel_()
                        .id(it.id)
                        .installer(it)
                        .markChecked(installerId == it.id)
                        .click { _ ->
                            save(it.id)
                            requestModelBuild()
                        }
                )
            }
        }

        if (isMIUI() && !isMiuiOptimizationDisabled()) {
            DeviceMiuiSheet.newInstance().show(childFragmentManager, DeviceMiuiSheet.TAG)
        }
    }

    private fun save(installerId: Int) {
        when (installerId) {
            0 -> {
                if (isMIUI() && !isMiuiOptimizationDisabled()) {
                    DeviceMiuiSheet.newInstance().show(childFragmentManager, DeviceMiuiSheet.TAG)
                }
                this.installerId = installerId
                save(PREFERENCE_INSTALLER_ID, installerId)
            }
            2 -> {
                if (checkRootAvailability()) {
                    this.installerId = installerId
                    save(PREFERENCE_INSTALLER_ID, installerId)
                } else {
                    showDialog(
                        R.string.action_installations,
                        R.string.installer_root_unavailable
                    )
                }
            }
            3 -> {
                if (checkServicesAvailability()) {
                    this.installerId = installerId
                    save(PREFERENCE_INSTALLER_ID, installerId)
                } else {
                    showDialog(
                        R.string.action_installations,
                        R.string.installer_service_unavailable
                    )
                }
            }
            else -> {
                this.installerId = installerId
                save(PREFERENCE_INSTALLER_ID, installerId)
            }
        }
    }

    private fun loadInstallersFromAssets(): List<Installer> {
        val inputStream = requireContext().assets.open("installers.json")
        val bytes = ByteArray(inputStream.available())
        inputStream.read(bytes)
        inputStream.close()

        val json = String(bytes, StandardCharsets.UTF_8)
        return gson.fromJson<MutableList<Installer>?>(
            json,
            object : TypeToken<MutableList<Installer?>?>() {}.type
        )
    }

    private fun checkRootAvailability(): Boolean {
        return Shell.getShell().isRoot
    }

    private fun checkServicesAvailability(): Boolean {
        val isInstalled = PackageUtil.isInstalled(
            requireContext(),
            ServiceInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME
        )

        val isCorrectVersionInstalled =
            PackageUtil.isInstalled(
                requireContext(),
                ServiceInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME,
                if (BuildConfig.VERSION_CODE < 31) 8 else 9
            )

        return isInstalled && isCorrectVersionInstalled
    }
}