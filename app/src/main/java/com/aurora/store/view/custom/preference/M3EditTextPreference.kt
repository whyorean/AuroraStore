package com.aurora.store.view.custom.preference

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class M3EditTextPreference : EditTextPreferenceDialogFragmentCompat() {

    companion object {
        const val PREFERENCE_DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG"

        fun newInstance(key: String?): M3EditTextPreference {
            val fragment = M3EditTextPreference()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(preference.dialogTitle)
            .setIcon(preference.dialogIcon)
            .setPositiveButton(preference.positiveButtonText, this)
            .setNegativeButton(preference.negativeButtonText, this)

        val contentView = onCreateDialogView(requireContext())
        if (contentView != null) {
            onBindDialogView(contentView)
            builder.setView(contentView)
        } else {
            builder.setMessage(preference.dialogMessage)
        }

        onPrepareDialogBuilder(builder)
        return builder.create()
    }


    override fun onResume() {
        super.onResume()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
}
