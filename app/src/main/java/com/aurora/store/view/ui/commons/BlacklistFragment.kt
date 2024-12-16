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
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.Constants
import com.aurora.extensions.toast
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.databinding.FragmentGenericWithSearchBinding
import com.aurora.store.view.epoxy.views.BlackListViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.all.BlacklistViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class BlacklistFragment : BaseFragment<FragmentGenericWithSearchBinding>() {

    private val viewModel: BlacklistViewModel by viewModels()

    private val startForDocumentImport =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it != null) importBlacklist(it) else toast(R.string.toast_black_import_failed)
        }
    private val startForDocumentExport =
        registerForActivityResult(ActivityResultContracts.CreateDocument(Constants.JSON_MIME_TYPE)) {
            if (it != null) exportBlacklist(it) else toast(R.string.toast_black_export_failed)
        }

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
            imgActionSecondary.apply {
                visibility = View.VISIBLE
                setImageDrawable(
                    AppCompatResources.getDrawable(requireContext(), R.drawable.ic_menu)
                )
                setOnClickListener {
                    showMenu(it)
                }
            }

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
                            it.applicationInfo!!.loadLabel(requireContext().packageManager)
                                .contains(s, true) || it.packageName.contains(s, true)
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
                ) {
                }
            })
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.blacklistProvider.blacklist = viewModel.selected
    }

    private fun showMenu(anchor: View) {
        val popupMenu = PopupMenu(
            ContextThemeWrapper(
                requireContext(),
                R.style.AppTheme_PopupMenu
            ), anchor
        )

        val inflater: MenuInflater = popupMenu.menuInflater
        inflater.inflate(R.menu.menu_blacklist, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.action_import -> {
                    startForDocumentImport.launch(arrayOf(Constants.JSON_MIME_TYPE))
                    true
                }

                R.id.action_export -> {
                    startForDocumentExport.launch(
                        "aurora_store_blacklist_${Calendar.getInstance().time.time}.json"
                    )
                    true
                }

                R.id.action_select_all -> {
                    viewModel.selectAll()
                    binding.recycler.requestModelBuild()
                    true
                }

                R.id.action_remove_all -> {
                    viewModel.removeAll()
                    binding.recycler.requestModelBuild()
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
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

    private fun importBlacklist(uri: Uri) {
        viewModel.importBlacklist(requireContext(), uri)
        binding.recycler.requestModelBuild()
        toast(R.string.toast_black_import_success)
    }

    private fun exportBlacklist(uri: Uri) {
        viewModel.exportBlacklist(requireContext(), uri)
        toast(R.string.toast_black_export_success)
    }
}
