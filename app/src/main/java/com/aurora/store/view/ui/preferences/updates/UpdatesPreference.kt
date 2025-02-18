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

package com.aurora.store.view.ui.preferences.updates

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.aurora.store.R
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.model.UpdateMode
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_AURORA_ONLY
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_FDROID
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_EXTENDED
import com.aurora.store.util.save
import com.aurora.store.view.ui.preferences.BasePreferenceFragment
import com.aurora.store.viewmodel.all.UpdatesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpdatesPreference : BasePreferenceFragment() {

    private val viewModel: UpdatesViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_updates, rootKey)

        findPreference<ListPreference>(PREFERENCE_UPDATES_AUTO)?.setOnPreferenceChangeListener { _, newValue ->
            val updateRestrictionsPref = findPreference<Preference>(PREFERENCES_UPDATES_RESTRICTIONS)
            val updateCheckIntervalPref =
                findPreference<SeekBarPreference>(PREFERENCE_UPDATES_CHECK_INTERVAL)

            when (UpdateMode.entries[newValue.toString().toInt()]) {
                UpdateMode.DISABLED -> {
                    updateRestrictionsPref?.isEnabled = false
                    updateCheckIntervalPref?.isEnabled = false
                    viewModel.updateHelper.cancelAutomatedCheck()
                    requireContext().save(PREFERENCE_UPDATES_AUTO, 0)
                    true
                }

                UpdateMode.CHECK_AND_NOTIFY -> {
                    if (permissionProvider.isGranted(PermissionType.POST_NOTIFICATIONS)) {
                        updateRestrictionsPref?.isEnabled = true
                        updateCheckIntervalPref?.isEnabled = true
                        viewModel.updateHelper.scheduleAutomatedCheck()
                        true
                    } else {
                        permissionProvider.request(PermissionType.POST_NOTIFICATIONS) {
                            if (it) {
                                updateRestrictionsPref?.isEnabled = true
                                updateCheckIntervalPref?.isEnabled = true
                                requireContext().save(PREFERENCE_UPDATES_AUTO, 1)
                                viewModel.updateHelper.scheduleAutomatedCheck()
                                activity?.recreate()
                            }
                        }
                        false
                    }
                }

                UpdateMode.CHECK_AND_INSTALL -> {
                    if (permissionProvider.isGranted(PermissionType.DOZE_WHITELIST)) {
                        updateRestrictionsPref?.isEnabled = true
                        updateCheckIntervalPref?.isEnabled = true
                        viewModel.updateHelper.scheduleAutomatedCheck()
                        true
                    } else {
                        permissionProvider.request(PermissionType.DOZE_WHITELIST) {
                            if (it) {
                                updateRestrictionsPref?.isEnabled = true
                                updateCheckIntervalPref?.isEnabled = true
                                requireContext().save(PREFERENCE_UPDATES_AUTO, 2)
                                viewModel.updateHelper.scheduleAutomatedCheck()
                                activity?.recreate()
                            }
                        }
                        false
                    }
                }

                else -> false
            }
        }

        findPreference<Preference>(PREFERENCES_UPDATES_RESTRICTIONS)?.apply {
            isEnabled = Preferences.getInteger(requireContext(), PREFERENCE_UPDATES_AUTO) != 0
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.updatesRestrictionsDialog)
                true
            }
        }

        findPreference<SeekBarPreference>(PREFERENCE_UPDATES_CHECK_INTERVAL)?.apply {
            isEnabled = Preferences.getInteger(requireContext(), PREFERENCE_UPDATES_AUTO) != 0
            setOnPreferenceChangeListener { _, _ ->
                viewModel.updateHelper.updateAutomatedCheck()
                true
            }
        }

        findPreference<SwitchPreferenceCompat>(PREFERENCE_FILTER_AURORA_ONLY)
            ?.setOnPreferenceChangeListener { _, newValue ->
                findPreference<SwitchPreferenceCompat>(PREFERENCE_FILTER_FDROID)?.isEnabled =
                    !newValue.toString().toBoolean()
                viewModel.updateHelper.checkUpdatesNow()
                true
            }

        findPreference<SwitchPreferenceCompat>(PREFERENCE_FILTER_FDROID)?.apply {
            isEnabled = !Preferences.getBoolean(requireContext(), PREFERENCE_FILTER_AURORA_ONLY)
            setOnPreferenceChangeListener { _, _ ->
                viewModel.updateHelper.checkUpdatesNow()
                true
            }
        }

        findPreference<SwitchPreferenceCompat>(PREFERENCE_UPDATES_EXTENDED)
            ?.setOnPreferenceChangeListener { _, _ ->
                viewModel.updateHelper.checkUpdatesNow()
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
