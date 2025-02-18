/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.view.ui.preferences.updates

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.aurora.store.R
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS_BATTERY
import com.aurora.store.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS_IDLE
import com.aurora.store.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS_METERED
import com.aurora.store.viewmodel.preferences.UpdatesRestrictionsViewModel
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpdatesRestrictionsDialog : DialogFragment() {

    private val viewModel: UpdatesRestrictionsViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_auto_updates_restrictions, null)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pref_updates_restrictions_title)
            .setMessage(R.string.pref_updates_restrictions_desc)
            .setView(view)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> dialog?.dismiss()}
            .create()
    }

    override fun onResume() {
        super.onResume()
        context?.let { setupRestrictions(it) }
    }

    override fun onDestroy() {
        viewModel.updateHelper.updateAutomatedCheck()
        super.onDestroy()
    }

    private fun setupRestrictions(ctx: Context) {
        dialog?.findViewById<MaterialCheckBox>(R.id.checkboxMetered)?.apply {
            isChecked = Preferences.getBoolean(ctx, PREFERENCES_UPDATES_RESTRICTIONS_METERED, true)
            setOnCheckedChangeListener { _, isChecked ->
                Preferences.putBoolean(ctx, PREFERENCES_UPDATES_RESTRICTIONS_METERED, isChecked)
            }
        }

        dialog?.findViewById<MaterialCheckBox>(R.id.checkboxIdle)?.apply {
            isChecked = Preferences.getBoolean(ctx, PREFERENCES_UPDATES_RESTRICTIONS_IDLE, true)
            setOnCheckedChangeListener { _, isChecked ->
                Preferences.putBoolean(ctx, PREFERENCES_UPDATES_RESTRICTIONS_IDLE, isChecked)
            }
        }

        dialog?.findViewById<MaterialCheckBox>(R.id.checkboxBattery)?.apply {
            isChecked = Preferences.getBoolean(ctx, PREFERENCES_UPDATES_RESTRICTIONS_BATTERY, true)
            setOnCheckedChangeListener { _, isChecked ->
                Preferences.putBoolean(ctx, PREFERENCES_UPDATES_RESTRICTIONS_BATTERY, isChecked)
            }
        }
    }
}
