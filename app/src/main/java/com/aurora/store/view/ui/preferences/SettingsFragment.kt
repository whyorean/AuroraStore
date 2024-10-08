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
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aurora.store.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_settings, rootKey)

        findPreference<Preference>("pref_filter")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.filterPreference)
            true
        }
        findPreference<Preference>("pref_install")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.installationPreference)
            true
        }
        findPreference<Preference>("pref_ui")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.UIPreference)
            true
        }
        findPreference<Preference>("pref_network")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.networkPreference)
            true
        }
        findPreference<Preference>("pref_updates")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.updatesPreference)
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.title_settings)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }
}
