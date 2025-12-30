package com.aurora.store.view.ui.splash

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.Constants.PACKAGE_NAME_PLAY_STORE
import com.aurora.extensions.TAG
import com.aurora.extensions.getPackageName
import com.aurora.extensions.navigate
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.store.R
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.data.model.AuthState
import com.aurora.store.databinding.FragmentSplashBinding
import com.aurora.store.util.CertUtil.GOOGLE_ACCOUNT_TYPE
import com.aurora.store.util.CertUtil.GOOGLE_PLAY_AUTH_TOKEN_TYPE
import com.aurora.store.util.CertUtil.GOOGLE_PLAY_CERT
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_MICROG_AUTH
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseFlavouredSplashFragment : BaseFragment<FragmentSplashBinding>() {

    val viewModel: AuthViewModel by activityViewModels()

    val canLoginWithMicroG: Boolean
        get() = PackageUtil.hasSupportedMicroGVariant(requireContext()) &&
                Preferences.getBoolean(requireContext(), PREFERENCE_MICROG_AUTH, true)

    val startForAccount =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val accountName = it.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                requestAuthTokenForGoogle(accountName)
            } else {
                resetActions()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!Preferences.getBoolean(requireContext(), PREFERENCE_INTRO)) {
            requireContext().navigate(Screen.Onboarding)
            activity?.finish()
            return
        }

        // Toolbar
        binding.toolbar.apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_blacklist_manager -> {
                        requireContext().navigate(Screen.Blacklist)
                    }

                    R.id.menu_spoof_manager -> requireContext().navigate(Screen.Spoof)

                    R.id.menu_settings -> {
                        findNavController().navigate(R.id.settingsFragment)
                    }

                    R.id.menu_about -> requireContext().navigate(Screen.About)
                }
                true
            }
        }

        attachActions()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collectLatest {
                when (it) {
                    AuthState.Init -> updateStatus(getString(R.string.session_init))

                    AuthState.Fetching -> {
                        updateStatus(getString(R.string.requesting_new_session))
                    }

                    AuthState.Valid -> {
                        val packageName =
                            requireActivity().intent.getPackageName(requireArguments())
                        if (packageName.isNullOrBlank()) {
                            navigateToDefaultTab()
                        } else {
                            requireArguments().remove("packageName")
                            openDetailsFragment(packageName)
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
                        val packageName =
                            requireActivity().intent.getPackageName(requireArguments())
                        if (packageName.isNullOrBlank()) {
                            navigateToDefaultTab()
                        } else {
                            requireArguments().remove("packageName")
                            openDetailsFragment(packageName)
                        }
                    }

                    AuthState.SignedOut -> {
                        updateStatus(getString(R.string.session_scrapped))
                        updateActionLayout(true)
                    }

                    AuthState.Verifying -> {
                        updateStatus(getString(R.string.verifying_new_session))
                    }

                    is AuthState.PendingAccountManager -> {
                        requestAuthTokenForGoogle(it.email, it.token)
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
        binding.layoutAction.isVisible = isVisible
        binding.toolbar.isVisible = isVisible
    }

    private fun navigateToDefaultTab() {
        val defaultDestination =
            Preferences.getInteger(requireContext(), PREFERENCE_DEFAULT_SELECTED_TAB)
        val directions =
            when (requireArguments().getInt("destinationId", defaultDestination)) {
                R.id.updatesFragment -> {
                    requireArguments().remove("destinationId")
                    SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
                }

                1 -> SplashFragmentDirections.actionSplashFragmentToGamesContainerFragment()
                2 -> SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
                else -> SplashFragmentDirections.actionSplashFragmentToNavigationApps()
            }
        requireActivity().viewModelStore.clear() // Clear ViewModelStore to avoid bugs with logout
        findNavController().navigate(directions)
    }

    private fun requestAuthTokenForGoogle(accountName: String, oldToken: String? = null) {
        try {
            if (oldToken != null) {
                // Invalidate the old token before requesting a new one
                AccountManager.get(requireContext()).invalidateAuthToken(
                    GOOGLE_ACCOUNT_TYPE,
                    oldToken
                )
            }

            AccountManager.get(requireContext())
                .getAuthToken(
                    Account(accountName, GOOGLE_ACCOUNT_TYPE),
                    GOOGLE_PLAY_AUTH_TOKEN_TYPE,
                    bundleOf(
                        "overridePackage" to PACKAGE_NAME_PLAY_STORE,
                        "overrideCertificate" to Base64.decode(GOOGLE_PLAY_CERT, Base64.DEFAULT)
                    ),
                    requireActivity(),
                    {
                        viewModel.buildGoogleAuthData(
                            accountName,
                            it.result.getString(AccountManager.KEY_AUTHTOKEN)!!,
                            AuthHelper.Token.AUTH
                        )
                    },
                    Handler(Looper.getMainLooper())
                )
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get authToken for Google login")
        }
    }

    open fun attachActions() {
        binding.btnAnonymous.addOnClickListener {
            if (viewModel.authState.value != AuthState.Fetching) {
                binding.btnAnonymous.updateProgress(true)
                viewModel.buildAnonymousAuthData()
            }
        }

        binding.btnGoogle.addOnClickListener {
            if (viewModel.authState.value != AuthState.Fetching) {
                binding.btnGoogle.updateProgress(true)
                if (canLoginWithMicroG) {
                    Log.i(TAG, "Found supported microG, trying to request credentials")
                    val accountIntent = AccountManager.newChooseAccountIntent(
                        null,
                        null,
                        arrayOf(GOOGLE_ACCOUNT_TYPE),
                        null,
                        null,
                        null,
                        null
                    )
                    startForAccount.launch(accountIntent)
                } else {
                    findNavController().navigate(R.id.googleFragment)
                }
            }
        }
    }

    open fun resetActions() {
        binding.btnGoogle.apply {
            updateProgress(false)
            isEnabled = true
        }

        binding.btnAnonymous.apply {
            updateProgress(false)
            isEnabled = true
        }
    }
}
