package com.aurora.store.view.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.aurora.extensions.areNotificationsEnabled
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.Event
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.model.UpdateMode
import com.aurora.store.data.work.CacheWorker
import com.aurora.store.databinding.FragmentOnboardingBinding
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.save
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.onboarding.MicroGViewModel
import com.aurora.store.viewmodel.onboarding.OnboardingPage
import com.aurora.store.viewmodel.onboarding.OnboardingViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

abstract class BaseFlavouredOnboardingFragment : BaseFragment<FragmentOnboardingBinding>() {
    // Shared ViewModels
    val microGViewModel: MicroGViewModel by activityViewModels()
    val onboardingViewModel: OnboardingViewModel by activityViewModels()

    var currentPage = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adjust layout margins for edgeToEdge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutBottom) { layout, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            layout.setPadding(0, 0, 0, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val isDefaultPrefLoaded = Preferences.getBoolean(requireContext(), PREFERENCE_DEFAULT)

        if (!isDefaultPrefLoaded) {
            save(PREFERENCE_DEFAULT, true)
            loadDefaultPreferences()

            // No onboarding for TV, proceed with defaults
            if (PackageUtil.isTv(view.context)) finishOnboarding()
        }

        val pages = onboardingPages()

        with(binding) {
            // ViewPager2
            with(viewpager2) {
                adapter = PagerAdapter(
                    childFragmentManager,
                    viewLifecycleOwner.lifecycle,
                    pages
                )
                isUserInputEnabled = false
                setCurrentItem(0, true)
                registerOnPageChangeCallback(object : OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        onboardingViewModel.setCurrentPage(
                            when (position) {
                                0 -> OnboardingPage.WELCOME
                                1 -> OnboardingPage.PERMISSIONS
                                2 -> OnboardingPage.GSF
                                else -> OnboardingPage.WELCOME
                            }
                        )
                        currentPage = position
                    }
                })
            }

            TabLayoutMediator(tabLayout, viewpager2, true) { tab, position ->
                tab.text = (position + 1).toString()
            }.attach()
        }

        updateBackwardButton(false)
        updateForwardButton(true)

        viewLifecycleOwner.lifecycleScope.launch {
            // Combine both relevant flows
            combine(
                microGViewModel.checked,
                onboardingViewModel.currentPage
            ) { isChecked, page -> isChecked to page }.collect { (isChecked, page) ->
                when (page) {
                    OnboardingPage.WELCOME -> {
                        updateBackwardButton(enabled = false)
                        updateForwardButton(enabled = true)
                    }

                    OnboardingPage.PERMISSIONS -> {
                        updateBackwardButton(enabled = true)
                        val isLastPage = pages.size == 2

                        updateForwardButton(
                            enabled = true,
                            resId = if (isLastPage) R.string.action_finish else R.string.action_next,
                            if (isLastPage) {
                                { finishOnboarding() }
                            } else {
                                null
                            }
                        )
                    }

                    OnboardingPage.GSF -> {
                        updateBackwardButton(enabled = true)

                        if (isChecked) {
                            val isInstalled = PackageUtil.isMicroGBundleInstalled(requireContext())
                            updateForwardButton(
                                enabled = isInstalled,
                                resId = R.string.action_finish,
                                action = if (isInstalled) {
                                    { finishOnboarding() }
                                } else {
                                    null
                                }
                            )
                        } else {
                            updateForwardButton(
                                enabled = true,
                                resId = R.string.action_skip,
                                action = { finishOnboarding() }
                            )
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            AuroraApp.events.installerEvent.collect { onEvent(it) }
        }
    }

    private fun updateBackwardButton(
        enabled: Boolean = true
    ) {
        with(binding.btnBackward) {
            isEnabled = enabled
            text = getString(R.string.action_back)
            setOnClickListener({
                binding.viewpager2.setCurrentItem(binding.viewpager2.currentItem - 1, true)
            })
        }
    }

    private fun updateForwardButton(
        enabled: Boolean = true,
        @StringRes resId: Int = R.string.action_next,
        action: ((View) -> Unit)? = null
    ) {
        with(binding.btnForward) {
            isEnabled = enabled
            text = getString(resId)
            setOnClickListener(action ?: {
                binding.viewpager2.setCurrentItem(binding.viewpager2.currentItem + 1, true)
            })
        }
    }

    private fun onEvent(event: Event) {
        when (event) {
            is InstallerEvent.Installed -> {
                if (PackageUtil.isMicroGBundleInstalled(requireContext())) {
                    with(binding.btnForward) {
                        isEnabled = true
                        text = getString(R.string.action_finish)
                        setOnClickListener {
                            finishOnboarding()
                        }
                    }
                }
            }

            is InstallerEvent.Failed -> {
                // Wrap up the installation process & log error message
            }

            else -> {

            }
        }
    }

    abstract fun loadDefaultPreferences()

    abstract fun onboardingPages(): List<Fragment>

    open fun finishOnboarding() {
        setupAutoUpdates()
        CacheWorker.scheduleAutomatedCacheCleanup(requireContext())
        Preferences.putBooleanNow(requireContext(), PREFERENCE_INTRO, true)

        // Restart the app to ensure all permissions are granted
        ProcessPhoenix.triggerRebirth(context)
    }

    open fun setupAutoUpdates() {
        val updateMode = when {
            requireContext().isIgnoringBatteryOptimizations() -> UpdateMode.CHECK_AND_INSTALL
            requireContext().areNotificationsEnabled() -> UpdateMode.CHECK_AND_NOTIFY
            else -> UpdateMode.DISABLED
        }

        save(PREFERENCE_UPDATES_AUTO, updateMode.ordinal)

        onboardingViewModel.updateHelper.scheduleAutomatedCheck()
    }

    internal class PagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        var items: List<Fragment>
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun createFragment(position: Int): Fragment {
            return items[position]
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }
}
