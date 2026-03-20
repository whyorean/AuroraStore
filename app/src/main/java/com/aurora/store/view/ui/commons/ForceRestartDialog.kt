package com.aurora.store.view.ui.commons

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.aurora.store.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForceRestartDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.force_restart_title)
            .setMessage(R.string.force_restart_summary)
            .setPositiveButton(getString(R.string.action_restart)) { _, _ ->
                ProcessPhoenix.triggerRebirth(requireContext())
            }
            .create()

    override fun onResume() {
        super.onResume()
        dialog?.setCancelable(false)
    }
}
