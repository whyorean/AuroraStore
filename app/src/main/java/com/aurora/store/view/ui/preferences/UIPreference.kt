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
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.aurora.store.R
import com.aurora.store.util.Preferences
import com.aurora.extensions.applyTheme
import com.aurora.store.util.save


class UIPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_ui, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val themePreference: ListPreference? = findPreference(Preferences.PREFERENCE_THEME_TYPE)
        themePreference?.let {
            it.setOnPreferenceChangeListener { _, newValue ->
                val themeId = Integer.parseInt(newValue.toString())
                val accentId = Preferences.getInteger(
                    requireContext(),
                    Preferences.PREFERENCE_THEME_ACCENT
                )

                save(Preferences.PREFERENCE_THEME_TYPE, themeId)

                applyTheme(themeId, shouldApplyTransition = false)

                SettingsActivity.shouldRestart = true
                true
            }
        }

        val accentPreference: ListPreference? = findPreference(Preferences.PREFERENCE_THEME_ACCENT)
        accentPreference?.let {
            it.setOnPreferenceChangeListener { _, newValue ->
                val themeId = Preferences.getInteger(
                    requireContext(),
                    Preferences.PREFERENCE_THEME_TYPE
                )
                val accentId = Integer.parseInt(newValue.toString())

                save(Preferences.PREFERENCE_THEME_ACCENT, accentId)

                applyTheme(themeId, shouldApplyTransition = false)

                SettingsActivity.shouldRestart = true
                true
            }
        }
    }
}
