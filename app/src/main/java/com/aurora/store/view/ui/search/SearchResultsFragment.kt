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

package com.aurora.store.view.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.hideKeyboard
import com.aurora.extensions.showKeyboard
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.AppStreamStash
import com.aurora.store.R
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.model.ViewState.Loading.getDataAs
import com.aurora.store.databinding.FragmentSearchResultBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.view.epoxy.controller.SearchCarouselController
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.search.SearchResultViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchResultsFragment : BaseFragment<FragmentSearchResultBinding>(),
    GenericCarouselController.Callbacks {

    private val viewModel: SearchResultViewModel by activityViewModels()
    private val controller = SearchCarouselController(this)

    private var query: String
        get() = requireArguments().getString("query").orEmpty()
        set(value) = requireArguments().putString("query", value)

    private var scrollListener: EndlessRecyclerOnScrollListener =
        object : EndlessRecyclerOnScrollListener(visibleThreshold = 4) {
            override fun onLoadMore(currentPage: Int) {
                viewModel.observe(query)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Search
        attachSearch()
        with(binding) {
            toolbar.apply {
                setNavigationOnClickListener {
                    binding.searchBar.hideKeyboard()
                    findNavController().navigateUp()
                }
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_clear -> {
                            binding.searchBar.text?.clear()
                            binding.searchBar.showKeyboard()
                        }

                        R.id.action_download -> findNavController().navigate(R.id.downloadFragment)
                    }
                    true
                }
            }

            recycler.setController(controller)
            recycler.addOnScrollListener(scrollListener)
        }

        with(viewModel) {
            search(query)

            liveData.observe(viewLifecycleOwner) {
                when (it) {
                    is ViewState.Loading -> {
                        controller.setData(null)
                    }

                    is ViewState.Success<*> -> {
                        val stash = it.getDataAs<AppStreamStash>()
                        controller.setData(stash[query])
                    }

                    else -> {}
                }
            }
        }
    }

    private fun attachSearch() {
        binding.searchBar.text = Editable.Factory.getInstance().newEditable(query)

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.toolbar.menu.findItem(R.id.action_clear)?.isVisible = s.isNotBlank()
            }

            override fun afterTextChanged(s: Editable) {}
        })

        binding.searchBar.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == KeyEvent.ACTION_DOWN
                || actionId == KeyEvent.KEYCODE_ENTER
            ) {

                query = binding.searchBar.text.toString()

                queryViewModel(query)

                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun queryViewModel(query: String) {
        scrollListener.resetPageCount()
        viewModel.search(query)
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {
        openStreamBrowseFragment(streamCluster)
    }

    override fun onClusterScrolled(streamCluster: StreamCluster) {
        viewModel.observeCluster(query, streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsFragment(app.packageName)
    }

    override fun onAppLongClick(app: App) {

    }
}
