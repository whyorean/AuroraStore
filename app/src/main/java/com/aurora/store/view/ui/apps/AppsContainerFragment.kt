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
import com.aurora.store.view.ui.commons.CategoryFragment
import com.aurora.store.view.ui.commons.EditorChoiceFragment
import com.aurora.store.view.ui.commons.ForYouFragment
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
        B.pager.adapter = ViewPagerAdapter(childFragmentManager, lifecycle, authData.isAnonymous)
        B.pager.isUserInputEnabled = false //Disable viewpager scroll to avoid scroll conflicts

        TabLayoutMediator(B.tabLayout, B.pager, true) { tab: TabLayout.Tab, position: Int ->
            when (position) {
                0 -> tab.text = "For you"
                1 -> tab.text = "Top charts"
                2 -> tab.text = "Categories"
                3 -> tab.text = "Editor's choice"
                else -> {
                }
            }
        }.attach()
    }

    internal class ViewPagerAdapter(
        fragment: FragmentManager,
        lifecycle: Lifecycle,
        private val isAnonymous: Boolean
    ) :
        FragmentStateAdapter(fragment, lifecycle) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ForYouFragment.newInstance(0)
                1 -> TopChartContainerFragment()
                2 -> CategoryFragment.newInstance(0)
                3 -> EditorChoiceFragment.newInstance(0)
                else -> Fragment()
            }
        }

        override fun getItemCount(): Int {
            return if (isAnonymous)
                3
            else
                4
        }
    }
}