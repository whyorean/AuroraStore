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

package com.aurora.store.view.ui.games

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aurora.store.MobileNavigationDirections
import com.aurora.store.R
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.FragmentAppsGamesBinding
import com.aurora.store.util.Preferences
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.view.ui.commons.CategoryFragment
import com.aurora.store.view.ui.commons.EditorChoiceFragment
import com.aurora.store.view.ui.commons.ForYouFragment
import com.aurora.store.view.ui.commons.TopChartContainerFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GamesContainerFragment : BaseFragment<FragmentAppsGamesBinding>() {

    @Inject
    lateinit var authProvider: AuthProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.apply {
            title = getString(R.string.title_games)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_download_manager -> {
                        findNavController().navigate(R.id.downloadFragment)
                    }

                    R.id.menu_more -> {
                        findNavController().navigate(
                            MobileNavigationDirections.actionGlobalMoreDialogFragment()
                        )
                    }
                }
                true
            }
        }

        // ViewPager
        val isForYouEnabled = Preferences.getBoolean(
            requireContext(),
            Preferences.PREFERENCE_FOR_YOU
        )

        binding.pager.adapter = ViewPagerAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle,
            !authProvider.isAnonymous,
            isForYouEnabled
        )

        binding.pager.isUserInputEnabled = false

        val tabTitles: MutableList<String> = mutableListOf<String>().apply {
            if (isForYouEnabled) {
                add(getString(R.string.tab_for_you))
            }

            add(getString(R.string.tab_top_charts))
            add(getString(R.string.tab_categories))

            if (!authProvider.isAnonymous) {
                add(getString(R.string.tab_editor_choice))
            }
        }

        TabLayoutMediator(
            binding.tabLayout,
            binding.pager,
            true
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = tabTitles[position]
        }.attach()

        binding.searchFab.setOnClickListener {
            findNavController().navigate(R.id.searchSuggestionFragment)
        }
    }

    override fun onDestroyView() {
        binding.pager.adapter = null
        super.onDestroyView()
    }

    internal class ViewPagerAdapter(
        fragment: FragmentManager,
        lifecycle: Lifecycle,
        private val isGoogleAccount: Boolean,
        private val isForYouEnabled: Boolean
    ) :
        FragmentStateAdapter(fragment, lifecycle) {
        private val tabFragments: MutableList<Fragment> = mutableListOf<Fragment>().apply {
            if (isForYouEnabled) {
                add(ForYouFragment.newInstance(1))
            }

            add(TopChartContainerFragment.newInstance(1))
            add(CategoryFragment.newInstance(1))

            if (isGoogleAccount) {
                add(EditorChoiceFragment.newInstance(1))
            }
        }

        override fun createFragment(position: Int): Fragment {
            return tabFragments[position]
        }

        override fun getItemCount(): Int {
            return tabFragments.size
        }
    }
}
