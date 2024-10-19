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
import androidx.preference.ListPreference
import androidx.preference.SeekBarPreference
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.store.MobileNavigationDirections
import com.aurora.store.R
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UpdatesPreference : BasePreferenceFragment() {

    @Inject
    lateinit var updateHelper: UpdateHelper

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_updates, rootKey)

        findPreference<ListPreference>(PREFERENCE_UPDATES_AUTO)
            ?.setOnPreferenceChangeListener { _, newValue ->
                val value = newValue.toString().toInt()
                when (value) {
                    0 -> updateHelper.cancelAutomatedCheck()
                    1 -> updateHelper.scheduleAutomatedCheck()
                    else -> {
                        if (requireContext().isIgnoringBatteryOptimizations()) {
                            updateHelper.scheduleAutomatedCheck()
                            return@setOnPreferenceChangeListener true
                        } else {
                            findNavController().navigate(
                                MobileNavigationDirections.actionGlobalDozeWarningSheet(true)
                            )
                        }
                    }
                }
                value != 2
            }

        findPreference<SeekBarPreference>(PREFERENCE_UPDATES_CHECK_INTERVAL)
            ?.setOnPreferenceChangeListener { _, _ ->
                updateHelper.updateAutomatedCheck()
                true
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.title_updates)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }
}
