package com.aurora.store.view.ui.sheets

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.aurora.extensions.toast
import com.aurora.store.data.model.PermissionType
import com.aurora.store.R
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.databinding.SheetDozeWarningBinding
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.save
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DozeWarningSheet : BaseDialogSheet<SheetDozeWarningBinding>() {

    @Inject
    lateinit var updateHelper: UpdateHelper

    private val args: DozeWarningSheetArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSecondary.setOnClickListener { dismissAllowingStateLoss() }

        binding.btnPrimary.setOnClickListener {
            permissionProvider.request(PermissionType.DOZE_WHITELIST) {
                if (it) {
                    if (args.enableAutoUpdate) {
                        requireContext().save(PREFERENCE_UPDATES_AUTO, 2)
                        updateHelper.scheduleAutomatedCheck()
                    }
                    toast(R.string.toast_permission_granted)
                    activity?.recreate()
                } else {
                    toast(R.string.permissions_denied)
                }
                dismissAllowingStateLoss()
            }
        }
    }
}
