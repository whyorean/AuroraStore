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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.store.R
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.FragmentTopChartBinding


class TopChartContainerFragment : Fragment() {

    private lateinit var B: FragmentTopChartBinding

    private lateinit var authData: AuthData

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        B = FragmentTopChartBinding.bind(
            inflater.inflate(
                R.layout.fragment_top_chart,
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
        B.pager.adapter = ViewPagerAdapter(childFragmentManager, lifecycle)
        B.topTabGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.tab_top_free -> B.pager.setCurrentItem(0, true)
                R.id.tab_top_grossing -> B.pager.setCurrentItem(1, true)
                R.id.tab_trending -> B.pager.setCurrentItem(2, true)
                R.id.tab_top_paid -> B.pager.setCurrentItem(3, true)
            }
        }

        B.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> B.topTabGroup.check(R.id.tab_top_free)
                    1 -> B.topTabGroup.check(R.id.tab_top_grossing)
                    2 -> B.topTabGroup.check(R.id.tab_trending)
                    3 -> B.topTabGroup.check(R.id.tab_top_paid)
                }
            }
        })
    }

    internal class ViewPagerAdapter(fragment: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragment, lifecycle) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> TopChartFragment.newInstance(1, 0)
                1 -> TopChartFragment.newInstance(1, 1)
                2 -> TopChartFragment.newInstance(1, 2)
                3 -> TopChartFragment.newInstance(1, 3)
                else -> Fragment()
            }
        }

        override fun getItemCount(): Int {
            return 4
        }
    }
}