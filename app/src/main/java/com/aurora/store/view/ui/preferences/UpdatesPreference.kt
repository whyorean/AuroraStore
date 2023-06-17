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
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.aurora.store.R
import com.aurora.store.data.work.UpdateWorker
import com.aurora.store.util.Log
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK

class UpdatesPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_updates, rootKey)

        findPreference<SwitchPreferenceCompat>(PREFERENCE_UPDATES_CHECK)
            ?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue.toString().toBoolean()) {
                    UpdateWorker.scheduleAutomatedCheck(requireContext())
                } else {
                    UpdateWorker.cancelAutomatedCheck(requireContext())
                }
                true
            }
    }
}