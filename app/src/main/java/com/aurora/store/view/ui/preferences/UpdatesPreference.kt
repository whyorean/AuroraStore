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
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.work.UpdateWorker
import com.aurora.store.util.CertUtil
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK
import com.aurora.store.util.Preferences.PREFERENCE_SELF_UPDATE
import com.aurora.store.util.save

class UpdatesPreference : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_updates, rootKey)

        findPreference<SwitchPreferenceCompat>(PREFERENCE_SELF_UPDATE)?.let {
            if (CertUtil.isFDroidApp(requireContext(), BuildConfig.APPLICATION_ID)) {
                it.isVisible = false
            }

            it.setOnPreferenceChangeListener { _, newValue ->
                save(PREFERENCE_SELF_UPDATE, newValue.toString().toBoolean())
                true
            }
        }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.title_updates)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }
}
