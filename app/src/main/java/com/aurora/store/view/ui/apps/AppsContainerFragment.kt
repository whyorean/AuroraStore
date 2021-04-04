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

package com.aurora.store.view.ui.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.store.R
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.FragmentAppsGamesBinding
import com.aurora.store.util.Preferences
import com.aurora.store.view.ui.commons.CategoryFragment
import com.aurora.store.view.ui.commons.EditorChoiceFragment
import com.aurora.store.view.ui.commons.ForYouFragment
import com.aurora.store.view.ui.commons.TopChartContainerFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class AppsContainerFragment : Fragment() {

    private lateinit var B: FragmentAppsGamesBinding
    private lateinit var authData: AuthData

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        B = FragmentAppsGamesBinding.bind(
            inflater.inflate(
                R.layout.fragment_apps_games,
                container,
                false
            )
        )
        return B.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authData = AuthProvider.with(requireContext()).getAuthData()
        setupViewPager()
    }

    private fun setupViewPager() {
        val isForYouEnabled = Preferences.getBoolean(
            requireContext(),
            Preferences.PREFERENCE_FOR_YOU
        )

        val isGoogleAccount = !authData.isAnonymous

        B.pager.adapter = ViewPagerAdapter(
            childFragmentManager,
            lifecycle,
            isGoogleAccount,
            isForYouEnabled
        )

        B.pager.isUserInputEnabled = false //Disable viewpager scroll to avoid scroll conflicts

        val tabTitles: MutableList<String> = mutableListOf<String>().apply {
            if (isForYouEnabled) {
                add(getString(R.string.tab_for_you))
            }

            add(getString(R.string.tab_top_charts))
            add(getString(R.string.tab_categories))

            if (isGoogleAccount) {
                add(getString(R.string.tab_editor_choice))
            }
        }

        TabLayoutMediator(B.tabLayout, B.pager, true) { tab: TabLayout.Tab, position: Int ->
            tab.text = tabTitles[position]
        }.attach()
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
                add(ForYouFragment.newInstance(0))
            }
            add(TopChartContainerFragment.newInstance(0))
            add(CategoryFragment.newInstance(0))

            if (isGoogleAccount) {
                add(EditorChoiceFragment.newInstance(0))
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