package com.aurora.store.view.ui.sheets

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.navArgs
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.extensions.isMAndAbove
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.data.work.UpdateWorker
import com.aurora.store.databinding.SheetDozeWarningBinding
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.save

class DozeWarningSheet : BaseDialogSheet<SheetDozeWarningBinding>() {

    private val args: DozeWarningSheetArgs by navArgs()

    private val startForDozeResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            context?.let {
                if (it.isIgnoringBatteryOptimizations()) {
                    if (args.enableAutoUpdate) {
                        requireContext().save(PREFERENCE_UPDATES_AUTO, 2)
                        UpdateWorker.scheduleAutomatedCheck(requireContext())
                    }
                    toast(R.string.toast_permission_granted)
                    activity?.recreate()
                } else {
                    toast(R.string.permissions_denied)
                }
                dismissAllowingStateLoss()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSecondary.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnPrimary.setOnClickListener {
            if (isMAndAbove()) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                startForDozeResult.launch(intent)
            }
        }
    }
}
