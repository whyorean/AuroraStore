package com.aurora.store.view.ui.account

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.aurora.store.R
import com.aurora.store.data.providers.AccountProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogoutDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_logout_confirmation_title)
            .setMessage(R.string.action_logout_confirmation_message)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> logout() }
            .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> dialog?.dismiss()}
            .create()
    }

    private fun logout() {
        AccountProvider.logout(requireContext())
        findNavController().navigate(LogoutDialogDirections.actionLogoutDialogToSplashFragment())
    }
}
