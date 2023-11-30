package com.aurora.store.view.ui.sheets

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.aurora.extensions.isMAndAbove
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.databinding.SheetDozeWarningBinding

class DozeWarningSheet : BaseBottomSheet() {

    private val startForDozeResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            context?.let {
                val powerManager = it.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (isMAndAbove() && powerManager.isIgnoringBatteryOptimizations(it.packageName)) {
                    toast(R.string.toast_permission_granted)
                    activity?.recreate()
                } else {
                    toast(R.string.permissions_denied)
                }
                dismissAllowingStateLoss()
            }
        }

    private var _binding: SheetDozeWarningBinding? = null
    private val binding get() = _binding!!

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetDozeWarningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnSecondary.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnPrimary.setOnClickListener { requestDozePermission() }
    }

    private fun requestDozePermission() {
        if (isMAndAbove()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            startForDozeResult.launch(intent)
        }
    }
}
