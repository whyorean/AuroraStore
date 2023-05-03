package com.aurora.store.view.ui.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.aurora.store.R

class AdvancedPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_advanced, rootKey)
    }
}
