package com.aurora.store.view.ui.dispenser

import android.app.Dialog
import android.os.Bundle
import android.util.Patterns
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.aurora.extensions.showKeyboard
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import com.aurora.store.util.save
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InputDispenserDialog: DialogFragment() {

    private val textInputLayout: TextInputLayout?
        get() = dialog?.findViewById(R.id.textInputLayout)

    private val dispensers: Set<String>
        get() = Preferences.getStringSet(requireContext(), PREFERENCE_DISPENSER_URLS)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_dispenser_input, null)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_dispenser_title)
            .setMessage(R.string.add_dispenser_summary)
            .setView(view)
            .setPositiveButton(getString(R.string.add)) { _, _ -> saveDispenserUrl() }
            .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> dialog?.dismiss()}
            .create()
    }

    override fun onResume() {
        super.onResume()
        textInputLayout?.editText?.showKeyboard()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun saveDispenserUrl() {
        val url = textInputLayout?.editText?.text?.toString()
        if (!url.isNullOrEmpty() && Patterns.WEB_URL.matcher(url).matches()) {
            val newSet = dispensers.toMutableSet().apply {
                add(url)
            }
            save(PREFERENCE_DISPENSER_URLS, newSet)
        } else {
            toast(R.string.add_dispenser_error)
        }
    }
}
