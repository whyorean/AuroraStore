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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.aurora.extensions.runOnUiThread
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.save
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NetworkPreference : BasePreferenceFragment() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_network, rootKey)

        findPreference<EditTextPreference>(Preferences.PREFERENCE_PROXY_URL)?.let {
            it.summary =
                Preferences.getString(requireContext(), Preferences.PREFERENCE_PROXY_URL, "")

            it.setOnPreferenceChangeListener { _, newValue ->
                val newProxyUrl = newValue.toString()
                val newProxyInfo = CommonUtil.parseProxyUrl(newProxyUrl)

                if (newProxyInfo == null) {
                    requireContext().toast(getString(R.string.toast_proxy_invalid))
                } else {
                    viewModel.saveProxyDetails(newProxyUrl, newProxyInfo)
                }
                false
            }
        }

        findPreference<SwitchPreferenceCompat>(Preferences.PREFERENCE_INSECURE_ANONYMOUS)?.let {
            it.setOnPreferenceChangeListener { _, _ ->
                runOnUiThread {
                    requireContext().toast(R.string.insecure_anonymous_apply)
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.proxyURL.filter { it.isNotBlank() }.collect { pURL ->
                findPreference<EditTextPreference>(Preferences.PREFERENCE_PROXY_URL)?.let {
                    it.summary = pURL
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.proxyInfo.collect {
                if (it == null) {
                    requireContext().toast(getText(R.string.toast_proxy_failed))
                } else {
                    requireContext().toast(getString(R.string.toast_proxy_success))
                }
            }
        }
    }
}
