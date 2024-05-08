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

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.runOnUiThread
import com.aurora.extensions.showDialog
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.Log
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLATION_DEVICE_OWNER
import com.aurora.store.util.save
import com.aurora.store.view.custom.preference.AuroraListPreference
import dagger.hilt.android.AndroidEntryPoint
import rikka.shizuku.Shizuku
import rikka.sui.Sui

@AndroidEntryPoint
class InstallationPreference : BasePreferenceFragment() {

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
                save(Preferences.PREFERENCE_INSTALLER_ID, 5)
                activity?.recreate()
            } else {
                showDialog(
                    R.string.action_installations,
                    R.string.installer_shizuku_unavailable
                )
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_installation, rootKey)

        findPreference<Preference>(PREFERENCE_INSTALLATION_DEVICE_OWNER)?.apply {
            val packageName = context.packageName
            val devicePolicyManager =
                context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

            isVisible = devicePolicyManager.isDeviceOwnerApp(packageName)
            setOnPreferenceClickListener {
                context.showDialog(
                    context.getString(R.string.pref_clear_device_owner_title),
                    context.getString(R.string.pref_clear_device_owner_desc),
                    { _: DialogInterface, _: Int ->
                        @Suppress("DEPRECATION")
                        devicePolicyManager.clearDeviceOwnerApp(packageName)
                        activity?.recreate()
                    },
                    { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                )
                true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.title_installation)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        if (isOAndAbove() && AppInstaller.hasShizukuOrSui(requireContext())) {
            Shizuku.addBinderReceivedListenerSticky(shizukuAliveListener)
            Shizuku.addBinderDeadListener(shizukuDeadListener)
            Shizuku.addRequestPermissionResultListener(shizukuResultListener)
        }

        val abandonPreference: Preference? =
            findPreference(Preferences.INSTALLATION_ABANDON_SESSION)

        abandonPreference?.let {
            it.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    CommonUtil.cleanupInstallationSessions(requireContext())
                    runOnUiThread {
                        requireContext().toast(R.string.toast_abandon_sessions)
                    }
                    false
                }
        }

        val installerPreference: AuroraListPreference? =
            findPreference(Preferences.PREFERENCE_INSTALLER_ID)

        installerPreference?.let {
            it.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val selectedId = Integer.parseInt(newValue as String)
                    if (selectedId == 2) {
                        if (AppInstaller.hasRootAccess()) {
                            save(Preferences.PREFERENCE_INSTALLER_ID, selectedId)
                            true
                        } else {
                            showDialog(
                                R.string.action_installations,
                                R.string.installer_root_unavailable
                            )
                            false
                        }
                    } else if (selectedId == 3) {
                        if (AppInstaller.hasAuroraService(requireContext())) {
                            save(Preferences.PREFERENCE_INSTALLER_ID, selectedId)
                            true
                        } else {
                            showDialog(
                                R.string.action_installations,
                                R.string.installer_service_unavailable
                            )
                            false
                        }
                    } else if (selectedId == 4) {
                        if (AppInstaller.hasAppManager(requireContext())) {
                            save(Preferences.PREFERENCE_INSTALLER_ID, selectedId)
                            true
                        } else {
                            showDialog(
                                R.string.action_installations,
                                R.string.installer_am_unavailable
                            )
                            false
                        }
                    } else if (selectedId == 5) {
                        if (isOAndAbove() && AppInstaller.hasShizukuOrSui(requireContext())) {
                            if (shizukuAlive && AppInstaller.hasShizukuPerm()) {
                                save(Preferences.PREFERENCE_INSTALLER_ID, selectedId)
                                true
                            } else if (shizukuAlive && !Shizuku.shouldShowRequestPermissionRationale()) {
                                Shizuku.requestPermission(9000)
                                false
                            } else {
                                showDialog(
                                    R.string.action_installations,
                                    R.string.installer_shizuku_unavailable
                                )
                                false
                            }
                        } else {
                            showDialog(
                                R.string.action_installations,
                                R.string.installer_shizuku_unavailable
                            )
                            false
                        }
                    } else {
                        save(Preferences.PREFERENCE_INSTALLER_ID, selectedId)
                        true
                    }
                }
        }
    }

    override fun onDestroy() {
        if (isOAndAbove() && AppInstaller.hasShizukuOrSui(requireContext())) {
            Shizuku.removeBinderReceivedListener(shizukuAliveListener)
            Shizuku.removeBinderDeadListener(shizukuDeadListener)
            Shizuku.removeRequestPermissionResultListener(shizukuResultListener)
        }
        super.onDestroy()
    }
}
