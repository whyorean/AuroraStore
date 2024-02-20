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
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.work.SelfUpdateWorker
import com.aurora.store.data.work.UpdateWorker
import com.aurora.store.util.CertUtil
import com.aurora.store.util.Preferences.PREFERENCE_SELF_UPDATE
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import com.aurora.store.util.save
import com.aurora.store.view.custom.preference.ListPreferenceMaterialDialogFragmentCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpdatesPreference : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_updates, rootKey)

        findPreference<SwitchPreferenceCompat>(PREFERENCE_SELF_UPDATE)?.let {
            if (CertUtil.isFDroidApp(requireContext(), BuildConfig.APPLICATION_ID)) {
                it.isVisible = false
            }

            it.setOnPreferenceChangeListener { _, newValue ->
                if (newValue.toString().toBoolean()) {
                    SelfUpdateWorker.scheduleAutomatedCheck(requireContext())
                } else {
                    SelfUpdateWorker.cancelAutomatedCheck(requireContext())
                }
                save(PREFERENCE_SELF_UPDATE, newValue.toString().toBoolean())
                true
            }
        }

        findPreference<ListPreference>(PREFERENCE_UPDATES_AUTO)
            ?.setOnPreferenceChangeListener { _, newValue ->
                when (newValue.toString().toInt()) {
                    0 -> UpdateWorker.cancelAutomatedCheck(requireContext())
                    else -> UpdateWorker.scheduleAutomatedCheck(requireContext())
                }
                true
            }

        findPreference<SeekBarPreference>(PREFERENCE_UPDATES_CHECK_INTERVAL)
            ?.setOnPreferenceChangeListener { _, _ ->
                UpdateWorker.updateAutomatedCheck(requireContext())
                true
            }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ListPreference) {
            val dialogFragment =
                ListPreferenceMaterialDialogFragmentCompat.newInstance(preference.getKey())
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(
                parentFragmentManager,
                ListPreferenceMaterialDialogFragmentCompat.PREFERENCE_DIALOG_FRAGMENT_TAG
            )
        } else {
            super.onDisplayPreferenceDialog(preference)
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
