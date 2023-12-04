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

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.isExternalStorageAccessible
import com.aurora.store.util.save
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DownloadPreference : PreferenceFragmentCompat() {
    private lateinit var startForStorageManagerResult: ActivityResultLauncher<Intent>
    private lateinit var startForPermissions: ActivityResultLauncher<String>

    private var downloadDirectoryPreference: Preference? = null
    private var downloadExternalPreference: SwitchPreferenceCompat? = null
    private var autoDeletePreference: SwitchPreferenceCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startForStorageManagerResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val state = isRAndAbove() && Environment.isExternalStorageManager()
                if (state) {
                    downloadDirectoryPreference?.summary =
                        PathUtil.getExternalPath(requireContext())
                } else {
                    notifyPermissionState(isRAndAbove() && Environment.isExternalStorageManager())
                }
            }

        startForPermissions =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    notifyPermissionState(it)
                } else {
                    downloadExternalPreference?.isChecked = false
                }
            }
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_download, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.pref_app_download)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        downloadDirectoryPreference = findPreference(Preferences.PREFERENCE_DOWNLOAD_DIRECTORY)
        downloadExternalPreference = findPreference(Preferences.PREFERENCE_DOWNLOAD_EXTERNAL)
        autoDeletePreference = findPreference(Preferences.PREFERENCE_AUTO_DELETE)

        downloadDirectoryPreference?.let { preference ->
            preference.summary = PathUtil.getExternalPath(requireContext())
            preference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { it, newValue ->
                    if (PathUtil.canWriteToDirectory(requireContext(), newValue.toString())) {
                        it.summary = newValue.toString()
                        save(Preferences.PREFERENCE_DOWNLOAD_DIRECTORY, newValue.toString())
                        true
                    } else {
                        toast(R.string.pref_download_directory_error)
                        false
                    }
                }
        }

        downloadExternalPreference?.let { switchPreferenceCompat ->
            switchPreferenceCompat.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val checked = newValue.toString().toBoolean()

                    if (checked) {
                        if (isRAndAbove() && !Environment.isExternalStorageManager()) {
                            startForStorageManagerResult.launch(
                                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            )
                        }

                        if (!isRAndAbove() && !isExternalStorageAccessible(requireContext())) {
                            startForPermissions.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                    autoDeletePreference?.let {
                        if (checked) {
                            it.isEnabled = true
                        } else {
                            it.isEnabled = false
                            it.isChecked = true
                        }
                    }

                    true
                }
        }
    }

    private fun notifyPermissionState(state: Boolean) {
        if (state) {
            toast(R.string.toast_permission_granted)
        } else {
            toast(R.string.permissions_denied)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        startForStorageManagerResult.unregister()
        startForPermissions.unregister()
    }
}
