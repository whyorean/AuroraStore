package com.aurora.store.view.ui.onboarding

import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.aurora.extensions.isSAndAbove
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.databinding.FragmentAppLinksBinding

class AppLinksFragment : Fragment(R.layout.fragment_app_links) {

    private var _binding: FragmentAppLinksBinding? = null
    private val binding get() = _binding!!

    private val playStoreDomain = "play.google.com"

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isSAndAbove() && playStoreDomainVerified()) {
                toast(R.string.app_link_enabled)
            }
            updateButtonState()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAppLinksBinding.bind(view)

        updateButtonState()
        if (isSAndAbove()) {
            binding.btnAction.setOnClickListener {
                val intent = Intent(
                    ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                    Uri.parse("package:${view.context.packageName}")
                )
                startForResult.launch(intent)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun updateButtonState() {
        if (isSAndAbove()) {
            if (playStoreDomainVerified()) {
                binding.btnAction.apply {
                    text = getString(R.string.action_enabled)
                    isEnabled = false
                }
            }
        } else {
            binding.btnAction.apply {
                text = getString(R.string.action_enabled)
                isEnabled = false
            }
        }
    }

    private fun playStoreDomainVerified(): Boolean {
        return if (isSAndAbove()) {
            val domainVerificationManager = requireContext().getSystemService(
                DomainVerificationManager::class.java
            )
            val userState = domainVerificationManager.getDomainVerificationUserState(
                requireContext().packageName
            )

            val playStoreDomain = userState?.hostToStateMap?.filterKeys { it == playStoreDomain }
            playStoreDomain?.values?.first() == DomainVerificationUserState.DOMAIN_STATE_SELECTED
        } else {
            true
        }
    }

}
