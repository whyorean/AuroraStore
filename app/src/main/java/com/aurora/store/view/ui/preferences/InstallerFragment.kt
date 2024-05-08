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

package com.aurora.store.view.ui.preferences

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.isMIUI
import com.aurora.extensions.isMiuiOptimizationDisabled
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.showDialog
import com.aurora.store.R
import com.aurora.store.data.installer.AMInstaller
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.installer.NativeInstaller
import com.aurora.store.data.installer.RootInstaller
import com.aurora.store.data.installer.ServiceInstaller
import com.aurora.store.data.installer.SessionInstaller
import com.aurora.store.data.installer.ShizukuInstaller
import com.aurora.store.data.model.Installer
import com.aurora.store.databinding.FragmentInstallerBinding
import com.aurora.store.util.Log
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.aurora.store.util.save
import com.aurora.store.view.epoxy.views.preference.InstallerViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import rikka.shizuku.Shizuku
import rikka.sui.Sui

@AndroidEntryPoint
class InstallerFragment : BaseFragment(R.layout.fragment_installer) {

    private var _binding: FragmentInstallerBinding? = null
    private val binding get() = _binding!!

    private var installerId: Int = 0

    private var shizukuAlive = Sui.isSui()
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
                binding.epoxyRecycler.requestModelBuild()
            } else {
                showDialog(
                    R.string.action_installations,
                    R.string.installer_shizuku_unavailable
                )
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentInstallerBinding.bind(view)

        // Toolbar
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        installerId = Preferences.getInteger(requireContext(), PREFERENCE_INSTALLER_ID)

        if (isOAndAbove() && AppInstaller.hasShizukuOrSui(requireContext())) {
            Shizuku.addBinderReceivedListenerSticky(shizukuAliveListener)
            Shizuku.addBinderDeadListener(shizukuDeadListener)
            Shizuku.addRequestPermissionResultListener(shizukuResultListener)
        }

        // RecyclerView
        binding.epoxyRecycler.withModels {
            setFilterDuplicates(true)
            getInstallers().forEach {
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
            findNavController().navigate(R.id.deviceMiuiSheet)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        if (isOAndAbove() && AppInstaller.hasShizukuOrSui(requireContext())) {
            Shizuku.removeBinderReceivedListener(shizukuAliveListener)
            Shizuku.removeBinderDeadListener(shizukuDeadListener)
            Shizuku.removeRequestPermissionResultListener(shizukuResultListener)
        }
        super.onDestroy()
    }

    private fun save(installerId: Int) {
        when (installerId) {
            0 -> {
                if (isMIUI() && !isMiuiOptimizationDisabled()) {
                    findNavController().navigate(R.id.deviceMiuiSheet)
                }
                this.installerId = installerId
                save(PREFERENCE_INSTALLER_ID, installerId)
            }
            2 -> {
                if (AppInstaller.hasRootAccess()) {
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
                if (AppInstaller.hasAuroraService(requireContext())) {
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
                if (AppInstaller.hasAppManager(requireContext())) {
                    this.installerId = installerId
                    save(PREFERENCE_INSTALLER_ID, installerId)
                } else {
                    showDialog(
                        R.string.action_installations,
                        R.string.installer_am_unavailable
                    )
                }
            }
            5 -> {
                if (isOAndAbove() && AppInstaller.hasShizukuOrSui(requireContext())) {
                    if (shizukuAlive && AppInstaller.hasShizukuPerm()) {
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

    private fun getInstallers(): List<Installer> {
        val installers = mutableListOf(
            SessionInstaller.getInstallerInfo(requireContext()),
            NativeInstaller.getInstallerInfo(requireContext())
        )

        // 2
        if (AppInstaller.hasRootAccess()) {
            installers.add(RootInstaller.getInstallerInfo(requireContext()))
        }

        // 3
        if (AppInstaller.hasAuroraService(requireContext())) {
            installers.add(ServiceInstaller.getInstallerInfo(requireContext()))
        }

        // 4
        if (AppInstaller.hasAppManager(requireContext())) {
            installers.add(AMInstaller.getInstallerInfo(requireContext()))
        }

        // 5
        if (isOAndAbove() && AppInstaller.hasShizukuOrSui(requireContext())) {
            installers.add(ShizukuInstaller.getInstallerInfo(requireContext()))
        }

        return installers
    }

}
