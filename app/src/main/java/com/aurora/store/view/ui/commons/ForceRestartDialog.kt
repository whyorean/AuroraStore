package com.aurora.store.view.ui.commons

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.aurora.store.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess

@AndroidEntryPoint
class ForceRestartDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.force_restart_title)
            .setMessage(R.string.force_restart_summary)
            .setPositiveButton(getString(R.string.action_restart)) { _, _ -> restartApp() }
            .create()
    }

    override fun onResume() {
        super.onResume()
        dialog?.setCancelable(false)
    }

    private fun restartApp() {
        val context = requireContext()
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component

        val newIntent = Intent.makeRestartActivityTask(componentName).apply {
            setPackage(context.packageName)
        }

        startActivity(newIntent)
        exitProcess(0)
    }
}
