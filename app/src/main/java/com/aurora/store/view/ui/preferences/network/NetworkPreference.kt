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

package com.aurora.store.view.ui.preferences.network

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.aurora.extensions.runOnUiThread
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_MICROG_AUTH
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_URL
import com.aurora.store.util.Preferences.PREFERENCE_VENDING_VERSION
import com.aurora.store.util.save
import com.aurora.store.view.ui.preferences.BasePreferenceFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NetworkPreference : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_network, rootKey)

        findPreference<Preference>(Preferences.PREFERENCE_DISPENSER_URLS)?.apply {
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.dispenserFragment)
                true
            }
        }

        findPreference<Preference>(PREFERENCE_PROXY_URL)?.setOnPreferenceClickListener { _ ->
            findNavController().navigate(R.id.proxyURLDialog)
            false
        }

        findPreference<Preference>(PREFERENCE_VENDING_VERSION)?.let {
            it.setOnPreferenceChangeListener { _, newValue ->
                save(PREFERENCE_VENDING_VERSION, Integer.parseInt(newValue.toString()))
                runOnUiThread {
                    requireContext().toast(R.string.insecure_anonymous_apply)
                }
                true
            }
        }

        findPreference<SwitchPreferenceCompat>(PREFERENCE_MICROG_AUTH)?.isEnabled =
            PackageUtil.hasSupportedMicroG(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.pref_network_title)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }
}
