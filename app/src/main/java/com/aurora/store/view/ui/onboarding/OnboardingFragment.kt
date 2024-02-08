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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.aurora.extensions.isSAndAbove
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.work.SelfUpdateWorker
import com.aurora.store.data.work.UpdateWorker
import com.aurora.store.databinding.FragmentOnboardingBinding
import com.aurora.store.util.CertUtil
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.util.Preferences.PREFERENCE_DOWNLOAD_DIRECTORY
import com.aurora.store.util.Preferences.PREFERENCE_DOWNLOAD_EXTERNAL
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_AURORA_ONLY
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_FDROID
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_GOOGLE
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_SEARCH
import com.aurora.store.util.Preferences.PREFERENCE_FOR_YOU
import com.aurora.store.util.Preferences.PREFERENCE_INSECURE_ANONYMOUS
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_ENABLED
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_INFO
import com.aurora.store.util.Preferences.PREFERENCE_PROXY_URL
import com.aurora.store.util.Preferences.PREFERENCE_SELF_UPDATE
import com.aurora.store.util.Preferences.PREFERENCE_SIMILAR
import com.aurora.store.util.Preferences.PREFERENCE_THEME_ACCENT
import com.aurora.store.util.Preferences.PREFERENCE_THEME_TYPE
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_AUTO
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_EXTENDED
import com.aurora.store.util.Preferences.PREFERENCE_VENDING_VERSION
import com.aurora.store.util.save
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private var shouldSelfUpdate by Delegates.notNull<Boolean>()
    private var lastPosition = 0

    internal class PagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return WelcomeFragment()
                1 -> return InstallerFragment()
                2 -> return ThemeFragment()
                3 -> return AccentFragment()
                4 -> return AppLinksFragment()
                5 -> return PermissionsFragment()
            }
            return Fragment()
        }

        override fun getItemCount(): Int {
            return 6
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)

        shouldSelfUpdate = CertUtil.isFDroidApp(requireContext(), BuildConfig.APPLICATION_ID)

        val isDefaultPrefLoaded = Preferences.getBoolean(requireContext(), PREFERENCE_DEFAULT)
        if (!isDefaultPrefLoaded) {
            save(PREFERENCE_DEFAULT, true)
            loadDefaultPreferences()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun refreshButtonState() {
        binding.btnBackward.isEnabled = lastPosition != 0
        binding.btnForward.isEnabled = lastPosition != 5

        if (lastPosition == 5) {
            binding.btnForward.text = getString(R.string.action_finish)
            binding.btnForward.isEnabled = true
            binding.btnForward.setOnClickListener {
                save(PREFERENCE_INTRO, true)
                UpdateWorker.scheduleAutomatedCheck(requireContext())
                if (shouldSelfUpdate) SelfUpdateWorker.scheduleAutomatedCheck(requireContext())
                findNavController().navigate(
                    OnboardingFragmentDirections.actionOnboardingFragmentToSplashFragment()
                )
            }
        } else {
            binding.btnForward.text = getString(R.string.action_next)
            binding.btnForward.setOnClickListener {
                binding.viewpager2.setCurrentItem(
                    binding.viewpager2.currentItem + 1, true
                )
            }
        }
    }

    private fun loadDefaultPreferences() {
        /*Filters*/
        save(PREFERENCE_FILTER_AURORA_ONLY, false)
        save(PREFERENCE_FILTER_FDROID, true)
        save(PREFERENCE_FILTER_GOOGLE, false)
        save(PREFERENCE_FILTER_SEARCH, true)

        /*Downloader*/
        save(PREFERENCE_DOWNLOAD_EXTERNAL, false)
        save(PREFERENCE_DOWNLOAD_DIRECTORY, PathUtil.getExternalPath(requireContext()))

        /*Network*/
        save(PREFERENCE_INSECURE_ANONYMOUS, false)
        save(PREFERENCE_PROXY_ENABLED, false)
        save(PREFERENCE_PROXY_URL, "")
        save(PREFERENCE_PROXY_INFO, "{}")
        save(PREFERENCE_VENDING_VERSION, 0)

        /*Customization*/
        save(PREFERENCE_THEME_TYPE, 0)
        save(PREFERENCE_THEME_ACCENT, if (isSAndAbove()) 0 else 1)
        save(PREFERENCE_DEFAULT_SELECTED_TAB, 0)
        save(PREFERENCE_FOR_YOU, true)
        save(PREFERENCE_SIMILAR, true)

        /*Installer*/
        save(PREFERENCE_AUTO_DELETE, true)
        save(PREFERENCE_INSTALLER_ID, 0)

        /*Updates*/
        save(PREFERENCE_UPDATES_EXTENDED, false)
        save(PREFERENCE_UPDATES_AUTO, 2)
        save(PREFERENCE_UPDATES_CHECK_INTERVAL, 3)
        save(PREFERENCE_SELF_UPDATE, shouldSelfUpdate)
    }
}
