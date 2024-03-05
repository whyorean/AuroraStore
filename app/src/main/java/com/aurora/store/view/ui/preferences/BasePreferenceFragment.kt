package com.aurora.store.view.ui.preferences

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aurora.store.view.custom.preference.ListPreferenceMaterialDialogFragmentCompat

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ListPreference) {
            val dialogFragment = ListPreferenceMaterialDialogFragmentCompat.newInstance(preference.getKey())
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager,
                ListPreferenceMaterialDialogFragmentCompat.PREFERENCE_DIALOG_FRAGMENT_TAG
            )
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}
