package com.aurora.store.view.ui.about

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.aurora.store.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.about_aurora_store_title)
            .setMessage(R.string.about_aurora_store_summary)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> dialog?.dismiss() }
            .create()
    }
}
