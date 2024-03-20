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

package com.aurora.store.view.ui.all

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aurora.store.R
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.ActivityGenericPagerBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppsGamesFragment : Fragment(R.layout.activity_generic_pager) {

    private var _binding: ActivityGenericPagerBinding? = null
    private val binding: ActivityGenericPagerBinding
        get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ActivityGenericPagerBinding.bind(view)

        val authData = AuthProvider.with(view.context).getAuthData()

        // Toolbar
        binding.layoutActionToolbar.toolbar.apply {
            elevation = 0f
            title = getString(R.string.title_apps_games)
            navigationIcon = ContextCompat.getDrawable(view.context, R.drawable.ic_arrow_back)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        // ViewPager
        binding.pager.apply {
            isUserInputEnabled = false
            adapter = ViewPagerAdapter(childFragmentManager, lifecycle, authData.isAnonymous)
        }

        TabLayoutMediator(
            binding.tabLayout,
            binding.pager,
            true
        ) { tab: TabLayout.Tab, position: Int ->
            when (position) {
                0 -> tab.text = getString(R.string.title_installed)
                1 -> tab.text = getString(R.string.title_library)
                2 -> tab.text = getString(R.string.title_purchase_history)
                else -> {}
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    internal class ViewPagerAdapter(
        fragment: FragmentManager,
        lifecycle: Lifecycle,
        private val isAnonymous: Boolean
    ) :
        FragmentStateAdapter(fragment, lifecycle) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> InstalledAppsFragment.newInstance()
                1 -> LibraryAppsFragment.newInstance()
                2 -> PurchasedAppsFragment.newInstance()
                else -> Fragment()
            }
        }

        override fun getItemCount(): Int {
            return if (isAnonymous) 1 else 3
        }
    }
}
