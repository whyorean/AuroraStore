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

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.aurora.extensions.runOnUiThread
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_ENABLED
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_INFO
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_URL
import com.aurora.store.util.remove
import com.aurora.store.util.save
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NetworkPreference : BasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_network, rootKey)

        sharedPreferences = Preferences.getPrefs(requireContext())
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        findPreference<Preference>(Preferences.PREFERENCE_DISPENSER_URLS)?.apply {
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.dispenserFragment)
                true
            }
        }

        findPreference<SwitchPreferenceCompat>(PREFERENCE_PROXY_ENABLED)?.apply {
            isChecked = Preferences.getString(context, PREFERENCE_PROXY_URL).isNotBlank()
            setOnPreferenceChangeListener { _, newValue ->
                if (newValue.toString().toBoolean()) {
                    findNavController().navigate(R.id.proxyURLDialog)
                } else {
                    remove(PREFERENCE_PROXY_URL)
                    remove(PREFERENCE_PROXY_INFO)
                    findNavController().navigate(R.id.forceRestartDialog)
                }
                false
            }
        }

        findPreference<Preference>(Preferences.PREFERENCE_VENDING_VERSION)?.let {
            it.setOnPreferenceChangeListener { _, newValue ->
                save(Preferences.PREFERENCE_VENDING_VERSION, Integer.parseInt(newValue.toString()))
                runOnUiThread {
                    requireContext().toast(R.string.insecure_anonymous_apply)
                }
                true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.pref_network_title)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }

    override fun onDestroyView() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREFERENCE_PROXY_URL) {
            findPreference<SwitchPreferenceCompat>(PREFERENCE_PROXY_ENABLED)?.isChecked =
                Preferences.getString(requireContext(), PREFERENCE_PROXY_URL).isNotBlank()
        }
    }
}
