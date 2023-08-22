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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.RoundedCornersTransformation
import com.aurora.extensions.hide
import com.aurora.extensions.show
import com.aurora.store.R
import com.aurora.store.data.AuthState
import com.aurora.store.databinding.FragmentSplashBinding
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.viewmodel.auth.AuthViewModel

class SplashFragment : Fragment(R.layout.fragment_splash) {

    private var _binding: FragmentSplashBinding? = null
    private val binding: FragmentSplashBinding
        get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSplashBinding.bind(view)

        binding.imgIcon.load(R.mipmap.ic_launcher) {
            transformations(RoundedCornersTransformation(32F))
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
                }
                true
            }
        }

        if (!Preferences.getBoolean(requireContext(), PREFERENCE_INTRO)) {
            findNavController().navigate(
                SplashFragmentDirections.actionSplashFragmentToOnboardingFragment()
            )
        }

        attachActions()

        //Initial status
        updateStatus(getString(R.string.session_init))

        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                AuthState.Fetching -> {
                    updateStatus(getString(R.string.requesting_new_session))
                }

                AuthState.Valid -> {
                    navigateToDefaultTab()
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
                    navigateToDefaultTab()
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

        // Check authentication status
        viewModel.observe()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            binding.layoutAction.show()
            binding.layoutToolbarAction.toolbar.invalidateMenu()
        } else {
            binding.layoutAction.hide()
            binding.layoutToolbarAction.toolbar.menu.clear()
        }
    }

    private fun attachActions() {
        binding.btnAnonymous.addOnClickListener {
            if (viewModel.liveData.value != AuthState.Fetching) {
                binding.btnAnonymous.updateProgress(true)
                viewModel.buildAnonymousAuthData()
            }
        }

        binding.btnAnonymousInsecure.addOnClickListener {
            if (viewModel.liveData.value != AuthState.Fetching) {
                binding.btnAnonymousInsecure.updateProgress(true)
                viewModel.buildInSecureAnonymousAuthData()
            }
        }

        binding.btnGoogle.addOnClickListener {
            if (viewModel.liveData.value != AuthState.Fetching) {
                binding.btnGoogle.updateProgress(true)
                findNavController().navigate(
                    SplashFragmentDirections.actionSplashFragmentToGoogleFragment(
                        R.id.splashFragment
                    )
                )
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

        binding.btnAnonymousInsecure.apply {
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
        findNavController().navigate(directions)
    }
}
