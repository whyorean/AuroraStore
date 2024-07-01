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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.store.R
import com.aurora.store.data.room.favourites.Favourite
import com.aurora.store.databinding.FragmentFavouriteBinding
import com.aurora.store.view.epoxy.views.FavouriteViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.all.FavouriteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavouriteFragment : BaseFragment(R.layout.fragment_favourite) {

    private var _binding: FragmentFavouriteBinding? = null
    private val binding: FragmentFavouriteBinding
        get() = _binding!!

    private val viewModel: FavouriteViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentFavouriteBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favouritesList.collect {
                updateController(it)
            }
        }

        // Toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateController(favourites: List<Favourite>?) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            if (favourites == null) {
                for (i in 1..10) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else if(favourites.isEmpty()) {
                add(
                    NoAppViewModel_()
                        .id("no_app")
                        .icon(R.drawable.ic_favorite_unchecked)
                        .message(getString(R.string.details_no_favourites))
                )
            } else {
                favourites.forEach {
                    add(
                        FavouriteViewModel_()
                            .id(it.packageName.hashCode())
                            .favourite(it)
                            .onClick { _ -> openDetailsFragment(it.packageName) }
                            .onFavourite { _ -> viewModel.removeFavourite(it.packageName) }
                    )
                }
            }
        }
    }
}
