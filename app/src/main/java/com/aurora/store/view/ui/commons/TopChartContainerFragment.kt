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

package com.aurora.store.view.ui.commons

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.aurora.Constants
import com.aurora.store.R
import com.aurora.store.databinding.FragmentTopChartBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopChartContainerFragment : Fragment(R.layout.fragment_top_chart) {

    private var _binding: FragmentTopChartBinding? = null
    private val binding get() = _binding!!

    companion object {
        @JvmStatic
        fun newInstance(chartType: Int): TopChartContainerFragment {
            return TopChartContainerFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.TOP_CHART_TYPE, chartType)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTopChartBinding.bind(view)

        var chartType = 0
        val bundle = arguments
        if (bundle != null) {
            chartType = bundle.getInt(Constants.TOP_CHART_TYPE, 0)
        }

        // ViewPager
        binding.pager.adapter =
            ViewPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle, chartType)
        binding.topTabGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds[0]) {
                R.id.tab_top_free -> binding.pager.setCurrentItem(0, true)
                R.id.tab_top_grossing -> binding.pager.setCurrentItem(1, true)
                R.id.tab_trending -> binding.pager.setCurrentItem(2, true)
                R.id.tab_top_paid -> binding.pager.setCurrentItem(3, true)
            }
        }

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> binding.topTabGroup.check(R.id.tab_top_free)
                    1 -> binding.topTabGroup.check(R.id.tab_top_grossing)
                    2 -> binding.topTabGroup.check(R.id.tab_trending)
                    3 -> binding.topTabGroup.check(R.id.tab_top_paid)
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.pager.adapter = null
        _binding = null
    }

    internal class ViewPagerAdapter(
        fragment: FragmentManager,
        lifecycle: Lifecycle,
        chartType: Int
    ) :
        FragmentStateAdapter(fragment, lifecycle) {
        private val tabFragments: MutableList<TopChartFragment> = mutableListOf(
            TopChartFragment.newInstance(chartType, 0),
            TopChartFragment.newInstance(chartType, 1),
            TopChartFragment.newInstance(chartType, 2),
            TopChartFragment.newInstance(chartType, 3)
        )

        override fun createFragment(position: Int): Fragment {
            return tabFragments[position]
        }

        override fun getItemCount(): Int {
            return tabFragments.size
        }
    }
}
