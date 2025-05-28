package com.aurora.store.view.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.aurora.extensions.areNotificationsEnabled
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.store.R
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
import com.aurora.store.viewmodel.onboarding.OnboardingViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.jakewharton.processphoenix.ProcessPhoenix

abstract class BaseFlavouredOnboardingFragment : BaseFragment<FragmentOnboardingBinding>() {

    val viewModel: OnboardingViewModel by viewModels()

    var lastPosition = 0

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

        // ViewPager2
        binding.viewpager2.apply {
            adapter = PagerAdapter(
                childFragmentManager,
                viewLifecycleOwner.lifecycle,
                onboardingPages()
            )
            isUserInputEnabled = false
            setCurrentItem(0, true)
            registerOnPageChangeCallback(object : OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    activity?.runOnUiThread {
                        lastPosition = position
                        refreshButtonState()
                    }
                }
            })
        }

        TabLayoutMediator(binding.tabLayout, binding.viewpager2, true) { tab, position ->
            tab.text = (position + 1).toString()
        }.attach()

        binding.btnForward.setOnClickListener {
            binding.viewpager2.setCurrentItem(binding.viewpager2.currentItem + 1, true)
        }

        binding.btnBackward.setOnClickListener {
            binding.viewpager2.setCurrentItem(binding.viewpager2.currentItem - 1, true)
        }
    }

    fun refreshButtonState() {
        binding.btnBackward.isEnabled = lastPosition != 0
        binding.btnForward.isEnabled = lastPosition != 1

        if (lastPosition == 1) {
            binding.btnForward.text = getString(R.string.action_finish)
            binding.btnForward.isEnabled = true
            binding.btnForward.setOnClickListener { finishOnboarding() }
        } else {
            binding.btnForward.text = getString(R.string.action_next)
            binding.btnForward.setOnClickListener {
                binding.viewpager2.setCurrentItem(
                    binding.viewpager2.currentItem + 1, true
                )
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

        viewModel.updateHelper.scheduleAutomatedCheck()
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
