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
import androidx.preference.SwitchPreferenceCompat
import com.aurora.store.R
import com.aurora.store.util.Preferences


class DownloadPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_download, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val downloadExternalPreference: SwitchPreferenceCompat? =
            findPreference(Preferences.PREFERENCE_DOWNLOAD_EXTERNAL)

        val autoDeletePreference: SwitchPreferenceCompat? =
            findPreference(Preferences.PREFERENCE_AUTO_DELETE)


        downloadExternalPreference?.let { switchPreferenceCompat ->
            switchPreferenceCompat.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val checked = newValue.toString().toBoolean()
                    autoDeletePreference?.let {
                        if (checked) {
                            it.isEnabled = true
                        } else {
                            it.isEnabled = false
                            it.isChecked = true
                        }
                    }

                    true
                }
        }
    }
}