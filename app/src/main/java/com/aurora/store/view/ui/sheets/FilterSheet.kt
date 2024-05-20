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

import android.os.Bundle
import android.view.View
import com.aurora.store.R
import com.aurora.store.data.Filter
import com.aurora.store.data.providers.FilterProvider
import com.aurora.store.databinding.SheetFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FilterSheet : BottomSheetDialogFragment(R.layout.sheet_filter) {

    private var _binding: SheetFilterBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var filterProvider: FilterProvider

    private lateinit var filter: Filter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = SheetFilterBinding.bind(view)
        filter = filterProvider.getSavedFilter()

        attachSingleChips()
        attachMultipleChips()
        attachActions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun attachActions() {
        binding.btnPositive.setOnClickListener {
            filterProvider.saveFilter(filter)
            dismissAllowingStateLoss()
        }

        binding.btnNegative.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun attachSingleChips() {
        binding.filterGfs.apply {
            isChecked = filter.gsfDependentApps
            setOnCheckedChangeListener { _, checked ->
                isChecked = checked
                filter.gsfDependentApps = checked
            }
        }

        binding.filterPaid.apply {
            isChecked = filter.paidApps
            setOnCheckedChangeListener { _, checked ->
                isChecked = checked
                filter.paidApps = checked
            }
        }

        binding.filterAds.apply {
            isChecked = filter.appsWithAds
            setOnCheckedChangeListener { _, checked ->
                isChecked = checked
                filter.appsWithAds = checked
            }
        }
    }

    private fun attachMultipleChips() {
        val downloadLabels = resources.getStringArray(R.array.filterDownloadsLabels)
        val downloadValues = resources.getStringArray(R.array.filterDownloadsValues)
        val ratingLabels = resources.getStringArray(R.array.filterRatingLabels)
        val ratingValues = resources.getStringArray(R.array.filterRatingValues)
        var i = 0

        for (downloadLabel in downloadLabels) {
            val chip = Chip(requireContext())
            chip.id = i
            chip.text = downloadLabel
            chip.isChecked = filter.downloads == downloadValues[i].toInt()
            binding.downloadChips.addView(chip)
            i++
        }

        binding.downloadChips.setOnCheckedStateChangeListener { _, checkedIds ->
            filter.downloads = downloadValues[checkedIds[0]].toInt()
        }

        i = 0
        for (ratingLabel in ratingLabels) {
            val chip = Chip(requireContext())
            chip.id = i
            chip.text = ratingLabel
            chip.isChecked = filter.rating == ratingValues[i].toFloat()
            binding.ratingChips.addView(chip)
            i++
        }

        binding.ratingChips.setOnCheckedStateChangeListener { _, checkedIds ->
            filter.rating = ratingValues[checkedIds[0]].toFloat()
        }
    }
}
