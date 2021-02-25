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

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aurora.extensions.runOnUiThread
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.save
import com.aurora.store.view.custom.preference.AuroraListPreference
import com.topjohnwu.superuser.Shell


class InstallationPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_installation, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                        if (checkRoot()) {
                            save(Preferences.PREFERENCE_INSTALLER_ID, selectedId)
                            true
                        } else {
                            false
                        }
                    } else {
                        save(Preferences.PREFERENCE_INSTALLER_ID, selectedId)
                        true
                    }
                }
        }
    }

    private fun checkRoot(): Boolean {
        var isRootAvailable = false

        Shell.getShell {
            isRootAvailable = it.isRoot

            if (isRootAvailable)
                toast(R.string.installer_root_available)
            else
                toast(R.string.installer_root_unavailable)
        }

        return isRootAvailable
    }
}
