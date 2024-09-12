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
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.getSystemService
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aurora.extensions.runOnUiThread
import com.aurora.extensions.showDialog
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.Preferences.INSTALLATION_ABANDON_SESSION
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLATION_DEVICE_OWNER
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InstallationPreference : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_installation, rootKey)

        findPreference<Preference>(PREFERENCE_INSTALLER_ID)?.apply {
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.installerFragment)
                true
            }
        }

        findPreference<Preference>(INSTALLATION_ABANDON_SESSION)?.apply {
            setOnPreferenceClickListener {
                CommonUtil.cleanupInstallationSessions(requireContext())
                runOnUiThread {
                    requireContext().toast(R.string.toast_abandon_sessions)
                }
                false
            }
        }

        findPreference<Preference>(PREFERENCE_INSTALLATION_DEVICE_OWNER)?.apply {
            val packageName = context.packageName
            val devicePolicyManager = context.getSystemService<DevicePolicyManager>()

            isVisible = devicePolicyManager?.isDeviceOwnerApp(packageName) ?: false
            setOnPreferenceClickListener {
                context.showDialog(
                    context.getString(R.string.pref_clear_device_owner_title),
                    context.getString(R.string.pref_clear_device_owner_desc),
                    { _: DialogInterface, _: Int ->
                        @Suppress("DEPRECATION")
                        devicePolicyManager!!.clearDeviceOwnerApp(packageName)
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
    }
}
