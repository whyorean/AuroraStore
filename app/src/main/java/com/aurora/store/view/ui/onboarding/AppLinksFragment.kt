package com.aurora.store.view.ui.onboarding

import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.aurora.extensions.isSAndAbove
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.databinding.FragmentAppLinksBinding
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppLinksFragment : Fragment(R.layout.fragment_app_links) {

    private val TAG = AppLinksFragment::class.java.simpleName

    private var _binding: FragmentAppLinksBinding? = null
    private val binding get() = _binding!!

    private val playStoreDomain = "play.google.com"
    private val marketDomain = "market.android.com"
    private val amazonAppStoreDomain = "www.amazon.com"

    // AppLink buttons
    private lateinit var buttons: Map<String, MaterialButton>

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isSAndAbove() && buttons.keys.any { domainVerified(it) }) {
                toast(R.string.app_link_enabled)
            }
            updateButtonState()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAppLinksBinding.bind(view)

        buttons = mapOf(
            playStoreDomain to binding.playStoreButton,
            marketDomain to binding.marketButton,
            amazonAppStoreDomain to binding.amazonAppStoreButton
        )

        updateButtonState()
        if (isSAndAbove()) {
            buttons.values.forEach {
                it.setOnClickListener {
                    try {
                        val intent = Intent(
                            ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                            Uri.parse("package:${view.context.packageName}")
                        )
                        startForResult.launch(intent)
                    } catch (exception: Exception) {
                        Log.e(TAG, "Failed to open app links screen", exception)
                        toast(R.string.failed_app_link)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        buttons = emptyMap()
        _binding = null
        super.onDestroyView()
    }

    private fun updateButtonState() {
        if (isSAndAbove()) {
            buttons.forEach { (domain, button) ->
                button.apply {
                    text = if (domainVerified(domain)) {
                        getString(R.string.action_enabled)
                    } else {
                        getString(R.string.action_enable)
                    }
                    isEnabled = !domainVerified(domain)
                }
            }
        } else {
            buttons.forEach { (_, button) ->
                button.apply {
                    text = getString(R.string.action_enabled)
                    isEnabled = false
                }
            }
        }
    }

    private fun domainVerified(domain: String): Boolean {
        return if (isSAndAbove()) {
            val domainVerificationManager = requireContext().getSystemService(
                DomainVerificationManager::class.java
            )
            val userState = domainVerificationManager.getDomainVerificationUserState(
                requireContext().packageName
            )

            val domainMap = userState?.hostToStateMap?.filterKeys { it == domain }
            domainMap?.values?.first() == DomainVerificationUserState.DOMAIN_STATE_SELECTED
        } else {
            true
        }
    }

}
