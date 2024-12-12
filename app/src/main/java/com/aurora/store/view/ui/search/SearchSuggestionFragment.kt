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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.hideKeyboard
import com.aurora.extensions.showKeyboard
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.store.R
import com.aurora.store.databinding.FragmentSearchSuggestionBinding
import com.aurora.store.view.epoxy.views.SearchSuggestionViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.search.SearchSuggestionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchSuggestionFragment : BaseFragment<FragmentSearchSuggestionBinding>() {

    private val viewModel: SearchSuggestionViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.apply {
            setNavigationOnClickListener {
                binding.searchBar.hideKeyboard()
                findNavController().navigateUp()
            }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_clear -> binding.searchBar.text?.clear()
                    R.id.action_download -> findNavController().navigate(R.id.downloadFragment)
                }
                true
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchSuggestions.collectLatest {
                updateController(it)
            }
        }

        setupSearch()
    }

    override fun onResume() {
        super.onResume()
        binding.searchBar.showKeyboard()
    }

    private fun updateController(searchSuggestions: List<SearchSuggestEntry>) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            searchSuggestions.forEach {
                add(
                    SearchSuggestionViewModel_()
                        .id(it.title)
                        .entry(it)
                        .action { _ ->
                            updateQuery(it.title)
                        }
                        .click { _ ->
                            binding.searchBar.hideKeyboard()
                            search(it.title)
                        }
                )
            }
        }
    }

    private fun setupSearch() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isNotEmpty()) {
                    val query = s.toString()
                    if (query.isNotEmpty()) {
                        viewModel.observeStreamBundles(query)
                    }
                }
                binding.toolbar.menu.findItem(R.id.action_clear)?.isVisible = s.isNotBlank()
            }

            override fun afterTextChanged(s: Editable) {}
        })

        binding.searchBar.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == KeyEvent.ACTION_DOWN
                || actionId == KeyEvent.KEYCODE_ENTER
            ) {
                val query = binding.searchBar.text.toString()
                if (query.isNotEmpty()) {
                    binding.searchBar.hideKeyboard()
                    search(query)
                    return@setOnEditorActionListener true
                }
            }
            false
        }
    }

    private fun updateQuery(query: String) {
        binding.searchBar.text = Editable.Factory.getInstance().newEditable(query)
        binding.searchBar.setSelection(query.length)
    }

    private fun search(query: String) {
        findNavController().navigate(
            SearchSuggestionFragmentDirections
                .actionSearchSuggestionFragmentToSearchResultsFragment(query)
        )
    }
}
