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

import android.content.pm.PackageInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.store.AuroraApp
import com.aurora.store.data.event.BusEvent
import com.aurora.store.databinding.FragmentGenericWithSearchBinding
import com.aurora.store.view.epoxy.views.BlackListViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.all.BlacklistViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BlacklistFragment : BaseFragment<FragmentGenericWithSearchBinding>() {

    private val viewModel: BlacklistViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.packages.collect {
                updateController(it)
            }
        }

        // Toolbar
        binding.layoutToolbarNative.apply {
            imgActionPrimary.visibility = View.VISIBLE
            imgActionSecondary.visibility = View.GONE

            imgActionPrimary.setOnClickListener {
                viewModel.blacklistProvider.blacklist = viewModel.selected
                findNavController().navigateUp()
            }

            inputSearch.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s.isNullOrEmpty()) {
                        updateController(viewModel.packages.value)
                    } else {
                        val filteredPackages = viewModel.packages.value?.filter {
                            it.applicationInfo!!.loadLabel(requireContext().packageManager).contains(s, true)
                        }
                        updateController(filteredPackages)
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}
            })
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.blacklistProvider.blacklist = viewModel.selected
    }

    private fun updateController(packages: List<PackageInfo>?) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            if (packages == null) {
                for (i in 1..10) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                packages
                    .sortedByDescending { app ->
                        viewModel.blacklistProvider.isBlacklisted(app.packageName)
                    }
                    .forEach {
                        add(
                            BlackListViewModel_()
                                .id(it.packageName.hashCode())
                                .packageInfo(it)
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
