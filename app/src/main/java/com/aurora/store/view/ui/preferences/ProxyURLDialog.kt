package com.aurora.store.view.ui.preferences

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.showKeyboard
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_INFO
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_URL
import com.aurora.store.util.save
import com.aurora.store.viewmodel.preferences.ProxyURLViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProxyURLDialog: DialogFragment() {

    private val viewModel: ProxyURLViewModel by viewModels()

    private val textInputLayout: TextInputLayout?
        get() = dialog?.findViewById(R.id.textInputLayout)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_text_input_edit_text, null)
        val alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pref_network_proxy_url)
            .setMessage(R.string.pref_network_proxy_url_message)
            .setView(view)
            .setPositiveButton(getString(R.string.add), null)
            .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> dialog?.dismiss()}
            .create()

        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { saveProxyUrl() }
        }
        return alertDialog
    }

    @SuppressLint("AuthLeak") // False-positive
    override fun onResume() {
        super.onResume()
        textInputLayout?.editText?.apply {
            hint = "protocol://user:password@host:port"
            setText(Preferences.getString(context, PREFERENCE_PROXY_URL))
            showKeyboard()
        }
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun saveProxyUrl() {
        val url = textInputLayout?.editText?.text?.toString()
        if (url.isNullOrEmpty()) {
            toast(R.string.add_dispenser_error)
            return
        }

        val proxyInfo = CommonUtil.parseProxyUrl(url)
        if (proxyInfo != null) {
            save(PREFERENCE_PROXY_URL, url)
            save(PREFERENCE_PROXY_INFO, viewModel.gson.toJson(proxyInfo))
            toast(R.string.toast_proxy_success)
            findNavController().navigate(R.id.forceRestartDialog)
            return
        } else {
            toast(R.string.toast_proxy_failed)
        }
    }
}
