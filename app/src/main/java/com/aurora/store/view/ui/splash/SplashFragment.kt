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

package com.aurora.store.view.ui.splash

import android.content.Intent
import android.net.UrlQuerySanitizer
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.hide
import com.aurora.extensions.show
import com.aurora.store.R
import com.aurora.store.data.model.AuthState
import com.aurora.store.databinding.FragmentSplashBinding
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding>() {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!Preferences.getBoolean(requireContext(), PREFERENCE_INTRO)) {
            findNavController().navigate(
                SplashFragmentDirections.actionSplashFragmentToOnboardingFragment()
            )
            return
        }

        // Toolbar
        binding.layoutToolbarAction.toolbar.apply {
            elevation = 0f
            inflateMenu(R.menu.menu_splash)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_blacklist_manager -> {
                        findNavController().navigate(R.id.blacklistFragment)
                    }

                    R.id.menu_spoof_manager -> {
                        findNavController().navigate(R.id.spoofFragment)
                    }

                    R.id.menu_settings -> {
                        findNavController().navigate(R.id.settingsFragment)
                    }

                    R.id.menu_about -> findNavController().navigate(R.id.aboutFragment)
                }
                true
            }
        }

        attachActions()

        // Show anonymous logins if we have dispenser URL
        binding.btnAnonymous.visibility = if (viewModel.authProvider.dispenserURL.isNullOrBlank())
            View.GONE
        else
            View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collectLatest {
                when (it) {
                    AuthState.Init -> updateStatus(getString(R.string.session_init))

                    AuthState.Fetching -> {
                        updateStatus(getString(R.string.requesting_new_session))
                    }

                    AuthState.Valid -> {
                        val packageName = getPackageName()
                        if (packageName.isBlank()) {
                            navigateToDefaultTab()
                        } else {
                            requireArguments().remove("packageName")
                            findNavController().navigate(
                                SplashFragmentDirections.actionSplashFragmentToAppDetailsFragment(
                                    packageName
                                )
                            )
                        }
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
                        val packageName = getPackageName()
                        if (packageName.isBlank()) {
                            navigateToDefaultTab()
                        } else {
                            requireArguments().remove("packageName")
                            findNavController().navigate(
                                SplashFragmentDirections.actionSplashFragmentToAppDetailsFragment(
                                    packageName
                                )
                            )
                        }
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
    }

    private fun updateStatus(string: String?) {
        activity?.runOnUiThread { binding.txtStatus.text = string }
    }

    private fun updateActionLayout(isVisible: Boolean) {
        if (isVisible) {
            binding.layoutAction.show()
            binding.layoutToolbarAction.toolbar.visibility = View.VISIBLE
        } else {
            binding.layoutAction.hide()
            binding.layoutToolbarAction.toolbar.visibility = View.GONE
        }
    }

    private fun attachActions() {
        binding.btnAnonymous.addOnClickListener {
            if (viewModel.authState.value != AuthState.Fetching) {
                binding.btnAnonymous.updateProgress(true)
                viewModel.buildAnonymousAuthData()
            }
        }

        binding.btnGoogle.addOnClickListener {
            if (viewModel.authState.value != AuthState.Fetching) {
                binding.btnGoogle.updateProgress(true)
                findNavController().navigate(R.id.googleFragment)
            }
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

    private fun navigateToDefaultTab() {
        val directions =
            when (Preferences.getInteger(requireContext(), PREFERENCE_DEFAULT_SELECTED_TAB)) {
                1 -> SplashFragmentDirections.actionSplashFragmentToGamesContainerFragment()
                2 -> SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
                else -> SplashFragmentDirections.actionSplashFragmentToNavigationApps()
            }
        activity?.viewModelStore?.clear() // Clear ViewModelStore to avoid bugs with logout
        findNavController().navigate(directions)
    }

    private fun getPackageName(): String {
        // Navigation component cannot handle market scheme as its missing a valid host
        return if (activity?.intent != null && activity?.intent?.scheme == "market") {
            requireActivity().intent.data!!.getQueryParameter("id") ?: ""
        } else if (activity?.intent != null && activity?.intent?.action == Intent.ACTION_SEND) {
            val clipData = activity?.intent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            UrlQuerySanitizer(clipData).getValue("id") ?: ""
        } else {
            requireArguments().getString("packageName") ?: ""
        }
    }
}
