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

package com.aurora.store.view.ui.details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.gplayapi.data.models.SearchBundle
import com.aurora.store.databinding.FragmentGenericWithToolbarBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.search.SearchResultViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DevAppsFragment : BaseFragment<FragmentGenericWithToolbarBinding>() {

    private val args: DevAppsFragmentArgs by navArgs()
    private val viewModel: SearchResultViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.liveData.observe(viewLifecycleOwner) {
            updateController(it)
        }

        // Toolbar
        binding.toolbar.apply {
            title = args.developerName
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        // Recycler View
        val endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                viewModel.liveData.value?.let { viewModel.next(it.subBundles) }
            }
        }
        binding.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)

        viewModel.observeSearchResults("pub:${args.developerName}")
    }

    private fun updateController(searchBundle: SearchBundle) {
        binding.recycler
            .withModels {
                setFilterDuplicates(true)
                searchBundle.appList
                    .filter { it.displayName.isNotEmpty() }
                    .forEach { app ->
                        add(
                            AppListViewModel_()
                                .id(app.id)
                                .app(app)
                                .click(View.OnClickListener {
                                    openDetailsFragment(app.packageName, app)
                                })
                        )
                    }

                if (searchBundle.subBundles.isNotEmpty()) {
                    add(
                        AppProgressViewModel_()
                            .id("progress")
                    )
                }
            }
    }
}
