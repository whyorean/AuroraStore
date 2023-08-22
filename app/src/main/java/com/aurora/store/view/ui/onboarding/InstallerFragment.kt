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

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.isMIUI
import com.aurora.extensions.isMiuiOptimizationDisabled
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.showDialog
import com.aurora.store.R
import com.aurora.store.data.installer.AppInstaller.Companion.hasAppManager
import com.aurora.store.data.installer.AppInstaller.Companion.hasAuroraService
import com.aurora.store.data.installer.AppInstaller.Companion.hasRootAccess
import com.aurora.store.data.installer.AppInstaller.Companion.hasShizuku
import com.aurora.store.data.installer.AppInstaller.Companion.hasShizukuPerm
import com.aurora.store.data.model.Installer
import com.aurora.store.databinding.FragmentOnboardingInstallerBinding
import com.aurora.store.util.Log
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.aurora.store.util.save
import com.aurora.store.view.epoxy.views.preference.InstallerViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.google.gson.reflect.TypeToken
import java.nio.charset.StandardCharsets
import rikka.shizuku.Shizuku


class InstallerFragment : BaseFragment() {

    private lateinit var B: FragmentOnboardingInstallerBinding

    var installerId: Int = 0

    private var shizukuAlive = false
    private val shizukuAliveListener = Shizuku.OnBinderReceivedListener {
        Log.d("ShizukuInstaller Alive!")
        shizukuAlive = true
    }
    private val shizukuDeadListener = Shizuku.OnBinderDeadListener {
        Log.d("ShizukuInstaller Dead!")
        shizukuAlive = false
    }

    private val shizukuResultListener =
        Shizuku.OnRequestPermissionResultListener { _: Int, result: Int ->
            if (result == PackageManager.PERMISSION_GRANTED) {
                this.installerId = 5
                save(PREFERENCE_INSTALLER_ID, 5)
                B.epoxyRecycler.requestModelBuild()
            } else {
                showDialog(
                    R.string.action_installations,
                    R.string.installer_shizuku_unavailable
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        if (hasShizuku(requireContext()) && isOAndAbove()) {
            Shizuku.addBinderReceivedListenerSticky(shizukuAliveListener)
            Shizuku.addBinderDeadListener(shizukuDeadListener)
            Shizuku.addRequestPermissionResultListener(shizukuResultListener)
        }

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

    override fun onDestroy() {
        if (hasShizuku(requireContext()) && isOAndAbove()) {
            Shizuku.removeBinderReceivedListener(shizukuAliveListener)
            Shizuku.removeBinderDeadListener(shizukuDeadListener)
            Shizuku.removeRequestPermissionResultListener(shizukuResultListener)
        }
        super.onDestroy()
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
            findNavController().navigate(
                OnboardingFragmentDirections.actionOnboardingFragmentToDeviceMiuiSheet()
            )
        }
    }

    private fun save(installerId: Int) {
        when (installerId) {
            0 -> {
                if (isMIUI() && !isMiuiOptimizationDisabled()) {
                    findNavController().navigate(
                        OnboardingFragmentDirections.actionOnboardingFragmentToDeviceMiuiSheet()
                    )
                }
                this.installerId = installerId
                save(PREFERENCE_INSTALLER_ID, installerId)
            }
            2 -> {
                if (hasRootAccess()) {
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
                if (hasAuroraService(requireContext())) {
                    this.installerId = installerId
                    save(PREFERENCE_INSTALLER_ID, installerId)
                } else {
                    showDialog(
                        R.string.action_installations,
                        R.string.installer_service_unavailable
                    )
                }
            }
            4 -> {
                if (hasAppManager(requireContext())) {
                    this.installerId = installerId
                    save(Preferences.PREFERENCE_INSTALLER_ID, installerId)
                } else {
                    showDialog(
                        R.string.action_installations,
                        R.string.installer_am_unavailable
                    )
                }
            }
            5 -> {
                if (hasShizuku(requireContext()) && isOAndAbove()) {
                    if (shizukuAlive && hasShizukuPerm()) {
                        this.installerId = installerId
                        save(PREFERENCE_INSTALLER_ID, installerId)
                    } else if (shizukuAlive && !Shizuku.shouldShowRequestPermissionRationale()) {
                        Shizuku.requestPermission(9000)
                    } else {
                        showDialog(
                            R.string.action_installations,
                            R.string.installer_shizuku_unavailable
                        )
                    }
                } else {
                    showDialog(
                        R.string.action_installations,
                        R.string.installer_shizuku_unavailable
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

}
