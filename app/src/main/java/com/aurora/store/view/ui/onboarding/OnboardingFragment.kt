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

package com.aurora.store.view.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.aurora.Constants
import com.aurora.extensions.areNotificationsEnabled
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.store.R
import com.aurora.store.data.model.UpdateMode
import com.aurora.store.data.work.CacheWorker
import com.aurora.store.databinding.FragmentOnboardingBinding
import com.aurora.store.util.CertUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_AURORA_ONLY
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_FDROID
import com.aurora.store.util.Preferences.PREFERENCE_FOR_YOU
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_THEME_STYLE
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_EXTENDED
import com.aurora.store.util.Preferences.PREFERENCE_VENDING_VERSION
import com.aurora.store.util.save
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.onboarding.OnboardingViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>() {

    private val viewModel: OnboardingViewModel by viewModels()

    private var lastPosition = 0

    internal class PagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return WelcomeFragment()
                1 -> return PermissionsFragment.newInstance()
                2 -> return AppLinksFragment()
            }
            return Fragment()
        }

        override fun getItemCount(): Int {
            return 2
        }
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
            adapter = PagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
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

    private fun finishOnboarding() {
        setupAutoUpdates()
        CacheWorker.scheduleAutomatedCacheCleanup(requireContext())
        Preferences.putBooleanNow(requireContext(), PREFERENCE_INTRO, true)

        // Restart the app to ensure all permissions are granted
        ProcessPhoenix.triggerRebirth(context)
    }

    private fun loadDefaultPreferences() {
        /*Filters*/
        save(PREFERENCE_FILTER_AURORA_ONLY, false)
        save(PREFERENCE_FILTER_FDROID, true)

        /*Network*/
        // TODO: Gather feedback and drop setting default dispenser for all builds
        if (!CertUtil.isAppGalleryApp(requireContext(), requireContext().packageName)) {
            save(PREFERENCE_DISPENSER_URLS, setOf(Constants.URL_DISPENSER))
        }
        save(PREFERENCE_VENDING_VERSION, 0)

        /*Customization*/
        save(PREFERENCE_THEME_STYLE, 0)
        save(PREFERENCE_DEFAULT_SELECTED_TAB, 0)
        save(PREFERENCE_FOR_YOU, true)

        /*Installer*/
        save(PREFERENCE_AUTO_DELETE, true)
        save(PREFERENCE_INSTALLER_ID, 0)

        /*Updates*/
        save(PREFERENCE_UPDATES_EXTENDED, false)
        save(PREFERENCE_UPDATES_CHECK_INTERVAL, 3)
    }

    private fun setupAutoUpdates() {
        val updateMode = when {
            requireContext().isIgnoringBatteryOptimizations() -> UpdateMode.CHECK_AND_INSTALL
            requireContext().areNotificationsEnabled() -> UpdateMode.CHECK_AND_NOTIFY
            else -> UpdateMode.DISABLED
        }
        save(PREFERENCE_UPDATES_AUTO, updateMode.ordinal)
        viewModel.updateHelper.scheduleAutomatedCheck()
    }
}
