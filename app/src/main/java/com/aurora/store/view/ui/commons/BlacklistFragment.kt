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
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.gplayapi.data.models.App
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.databinding.FragmentGenericWithToolbarBinding
import com.aurora.store.view.epoxy.views.BlackListViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.all.BlacklistViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BlacklistFragment : BaseFragment<FragmentGenericWithToolbarBinding>() {
    private val viewModel: BlacklistViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.blackListedApps.collect {
                updateController(it)
            }
        }

        // Toolbar
        binding.layoutToolbarNative.toolbar.apply {
            title = getString(R.string.title_blacklist_manager)
            navigationIcon = ContextCompat.getDrawable(view.context, R.drawable.ic_arrow_back)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.blacklistProvider.blacklist = viewModel.selected
    }

    private fun updateController(blackList: List<App>?) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            if (blackList == null) {
                for (i in 1..10) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                blackList
                    .sortedByDescending { app ->
                        viewModel.blacklistProvider.isBlacklisted(app.packageName)
                    }
                    .forEach {
                        add(
                            BlackListViewModel_()
                                .id(it.packageName.hashCode())
                                .app(it)
                                .markChecked(viewModel.selected.contains(it.packageName))
                                .checked { _, isChecked ->
                                    if (isChecked) {
                                        viewModel.selected.add(it.packageName)
                                        AuroraApp.events.send(BusEvent.Blacklisted(it.packageName))
                                    } else {
                                        viewModel.selected.remove(it.packageName)
                                    }

                                    requestModelBuild()
                                }
                        )
                    }
            }
        }
    }
}
