/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.view.ui.account

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.RoundedCornersTransformation
import com.aurora.extensions.browse
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.store.R
import com.aurora.store.data.AuthState
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.FragmentAccountBinding
import com.aurora.store.viewmodel.auth.AuthViewModel

class AccountFragment : Fragment(R.layout.fragment_account) {

    private var _binding: FragmentAccountBinding? = null
    private val binding: FragmentAccountBinding
        get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    private lateinit var authData: AuthData
    private lateinit var accountProvider: AccountProvider

    private val URL_TOS = "https://play.google.com/about/play-terms/"
    private val URL_LICENSE = "https://gitlab.com/AuroraOSS/AuroraStore/blob/master/LICENSE"
    private val URL_DISCLAIMER =
        "https://gitlab.com/AuroraOSS/AuroraStore/blob/master/DISCLAIMER.md"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAccountBinding.bind(view)

        authData = AuthProvider.with(view.context).getAuthData()
        accountProvider = AccountProvider.with(view.context)

        // Toolbar
        binding.layoutToolbarAction.txtTitle.text = getString(R.string.title_account_manager)
        binding.layoutToolbarAction.imgActionPrimary.setOnClickListener {
            findNavController().navigateUp()
        }

        // Chips
        view.context.apply {
            binding.chipDisclaimer.setOnClickListener { browse(URL_DISCLAIMER) }
            binding.chipLicense.setOnClickListener { browse(URL_LICENSE) }
            binding.chipTos.setOnClickListener { browse(URL_TOS) }
        }

        attachActions()

        updateContents()

        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                AuthState.Fetching -> {
                    updateStatus(getString(R.string.requesting_new_session))
                }

                AuthState.Valid -> {

                }

                AuthState.Available -> {
                    updateStatus(getString(R.string.session_verifying))
                    updateActionLayout(false)
                }

                AuthState.Unavailable -> {
                    updateStatus(getString(R.string.session_login))
                    updateActionLayout(true)
                }

                AuthState.SignedIn -> {
                    updateContents()
                }

                AuthState.SignedOut -> {
                    updateStatus(getString(R.string.session_scrapped))
                    updateActionLayout(true)
                }

                AuthState.Verifying -> {
                    updateStatus(getString(R.string.verifying_new_session))
                }

                is AuthState.Failed -> {
                    updateStatus(it.status)
                    updateActionLayout(true)
                    resetActions()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateContents() {
        if (accountProvider.isSignedIn()) {
            binding.viewFlipper.displayedChild = 1
            updateStatus(getString(R.string.session_good))
        } else {
            binding.viewFlipper.displayedChild = 0
            updateStatus(getString(R.string.session_enjoy))
        }

        updateUserProfile()
    }

    private fun updateStatus(string: String?) {
        activity?.runOnUiThread {
            binding.txtStatus.apply {
                text = string
            }
        }
    }

    private fun updateActionLayout(isVisible: Boolean) {
        if (isVisible) {
            binding.layoutAction.visibility = View.VISIBLE
        } else {
            binding.layoutAction.visibility = View.INVISIBLE
        }
    }

    private fun attachActions() {
        binding.btnAnonymous.updateProgress(false)
        binding.btnGoogle.updateProgress(false)

        binding.btnAnonymous.addOnClickListener {
            if (viewModel.liveData.value != AuthState.Fetching) {
                binding.btnAnonymous.updateProgress(true)
                viewModel.buildAnonymousAuthData()
            }
        }

        binding.btnGoogle.addOnClickListener {
            if (viewModel.liveData.value != AuthState.Fetching) {
                binding.btnGoogle.updateProgress(true)
                findNavController().navigate(
                    AccountFragmentDirections.actionAccountFragmentToGoogleFragment(
                        R.id.accountFragment
                    )
                )
            }
        }

        binding.btnLogout.addOnClickListener {
            AccountProvider.with(it.context).logout()
            binding.btnAnonymous.updateProgress(false)
            binding.btnGoogle.updateProgress(false)
            updateContents()
        }
    }

    private fun resetActions() {
        binding.btnGoogle.apply {
            updateProgress(false)
            isEnabled = true
        }

        binding.btnAnonymous.apply {
            updateProgress(false)
            isEnabled = true
        }
    }

    private fun updateUserProfile() {
        authData = AuthProvider.with(requireContext()).getAuthData()

        if (accountProvider.isSignedIn()) {
            authData.userProfile?.let {
                binding.imgAvatar.load(it.artwork.url) {
                    placeholder(R.drawable.bg_placeholder)
                    transformations(RoundedCornersTransformation(32F))
                }

                binding.txtName.text = if (authData.isAnonymous)
                    "Anonymous"
                else
                    it.name

                binding.txtEmail.text = if (authData.isAnonymous)
                    "anonymous@gmail.com"
                else
                    it.email
            }
        } else {
            binding.imgAvatar.load(R.mipmap.ic_launcher) {
                transformations(RoundedCornersTransformation(32F))
            }
            binding.txtName.text = getString(R.string.app_name)
            binding.txtEmail.text = getString(R.string.account_logged_out)
        }
    }
}
