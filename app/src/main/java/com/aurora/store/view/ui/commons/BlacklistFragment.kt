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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.aurora.store.R
import com.aurora.store.data.model.Black
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.store.view.epoxy.views.BlackListViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.all.BlacklistViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlacklistFragment : Fragment(R.layout.activity_generic_recycler) {

    private var _binding: ActivityGenericRecyclerBinding? = null
    private val binding: ActivityGenericRecyclerBinding
        get() = _binding!!

    private lateinit var VM: BlacklistViewModel
    private lateinit var blacklistProvider: BlacklistProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = ActivityGenericRecyclerBinding.bind(view)
        VM = ViewModelProvider(this)[BlacklistViewModel::class.java]
        blacklistProvider = BlacklistProvider.with(view.context)

        VM.liveData.observe(viewLifecycleOwner) {
            updateController(it.sortedByDescending { app ->
                blacklistProvider.isBlacklisted(app.packageName)
            })
        }

        // Toolbar
        binding.layoutToolbarAction.txtTitle.text = getString(R.string.title_blacklist_manager)
        binding.layoutToolbarAction.imgActionPrimary.setOnClickListener {
            findNavController().navigateUp()
        }

        updateController(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        blacklistProvider.save(VM.selected)
    }

    private fun updateController(blackList: List<Black>?) {
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
                blackList.forEach {
                    add(
                        BlackListViewModel_()
                            .id(it.packageName.hashCode())
                            .black(it)
                            .markChecked(VM.selected.contains(it.packageName))
                            .checked { _, isChecked ->
                                if (isChecked)
                                    VM.selected.add(it.packageName)
                                else
                                    VM.selected.remove(it.packageName)

                                requestModelBuild()
                            }
                    )
                }
            }
        }
    }
}
