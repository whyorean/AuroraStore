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

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.Constants
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.data.room.favourite.Favourite
import com.aurora.store.databinding.FragmentFavouriteBinding
import com.aurora.store.view.epoxy.views.FavouriteViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.all.FavouriteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class FavouriteFragment : BaseFragment<FragmentFavouriteBinding>() {
    private val viewModel: FavouriteViewModel by viewModels()

    private val startForDocumentImport =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it != null) importFavourites(it) else toast(R.string.toast_fav_import_failed)
        }
    private val startForDocumentExport =
        registerForActivityResult(ActivityResultContracts.CreateDocument(Constants.JSON_MIME_TYPE)) {
            if (it != null) exportFavourites(it) else toast(R.string.toast_fav_export_failed)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favouritesList.collect {
                updateController(it)
            }
        }

        // Toolbar
        binding.toolbar.apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_import -> startForDocumentImport.launch(arrayOf(Constants.JSON_MIME_TYPE))
                    R.id.action_export -> {
                        startForDocumentExport.launch(
                            "aurora_store_favourites_${Calendar.getInstance().time.time}.json"
                        )
                    }

                    else -> {}
                }
                true
            }
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
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
            } else if (favourites.isEmpty()) {
                add(
                    NoAppViewModel_()
                        .id("no_app")
                        .icon(R.drawable.ic_favorite_unchecked)
                        .message(R.string.details_no_favourites)
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

    private fun importFavourites(uri: Uri) {
        viewModel.importFavourites(requireContext(), uri)
        binding.recycler.requestModelBuild()
        toast(R.string.toast_fav_import_success)
    }

    private fun exportFavourites(uri: Uri) {
        viewModel.exportFavourites(requireContext(), uri)
        toast(R.string.toast_fav_export_success)
    }
}
