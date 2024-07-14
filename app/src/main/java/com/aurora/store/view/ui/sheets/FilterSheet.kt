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

package com.aurora.store.view.ui.sheets

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.aurora.store.R
import com.aurora.store.data.model.Filter
import com.aurora.store.data.providers.FilterProvider
import com.aurora.store.databinding.SheetFilterBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FilterSheet : BaseDialogSheet<SheetFilterBinding>() {

    @Inject
    lateinit var filterProvider: FilterProvider

    private lateinit var filter: Filter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filter = filterProvider.getSavedFilter()

        attachSingleChips()
        attachMultipleChips()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        filterProvider.saveFilter(filter)
    }

    private fun attachSingleChips() {
        binding.filterGfs.apply {
            isChecked = filter.gsfDependentApps
            setOnCheckedChangeListener { _, checked ->
                isChecked = checked
                filter = filter.copy(gsfDependentApps = checked)
            }
        }

        binding.filterPaid.apply {
            isChecked = filter.paidApps
            setOnCheckedChangeListener { _, checked ->
                isChecked = checked
                filter = filter.copy(paidApps = checked)
            }
        }

        binding.filterAds.apply {
            isChecked = filter.appsWithAds
            setOnCheckedChangeListener { _, checked ->
                isChecked = checked
                filter = filter.copy(appsWithAds = checked)
            }
        }
    }

    private fun attachMultipleChips() {
        val downloadLabels = resources.getStringArray(R.array.filterDownloadsLabels)
        val downloadValues = resources.getStringArray(R.array.filterDownloadsValues)
        val ratingLabels = resources.getStringArray(R.array.filterRatingLabels)
        val ratingValues = resources.getStringArray(R.array.filterRatingValues)

        downloadLabels.forEachIndexed { index, value ->
            val chip = Chip(requireContext())
            chip.id = index
            chip.text = value
            chip.isChecked = filter.downloads == downloadValues[index].toInt()
            binding.downloadChips.addView(chip)
        }

        binding.downloadChips.setOnCheckedStateChangeListener { _, checkedIds ->
            filter = filter.copy(downloads = downloadValues[checkedIds[0]].toInt())
        }

        ratingLabels.forEachIndexed { index, value ->
            val chip = Chip(requireActivity())
            chip.id = index
            chip.text = value
            chip.isChecked = filter.rating == ratingValues[index].toFloat()
            binding.ratingChips.addView(chip)
        }

        binding.ratingChips.setOnCheckedStateChangeListener { _, checkedIds ->
            filter = filter.copy(rating = ratingValues[checkedIds[0]].toFloat())
        }
    }
}
