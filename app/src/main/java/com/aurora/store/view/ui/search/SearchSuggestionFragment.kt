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
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.showKeyboard
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.store.R
import com.aurora.store.databinding.FragmentSearchSuggestionBinding
import com.aurora.store.view.epoxy.views.SearchSuggestionViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.search.SearchSuggestionViewModel
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchSuggestionFragment : BaseFragment<FragmentSearchSuggestionBinding>() {

    private val viewModel: SearchSuggestionViewModel by viewModels()

    private lateinit var searchView: TextInputEditText

    private var query: String = String()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.layoutToolbarSearch.apply {
            searchView = inputSearch
            imgActionPrimary.setOnClickListener {
                findNavController().navigateUp()
            }
            imgActionSecondary.setOnClickListener {
                findNavController().navigate(R.id.downloadFragment)
            }
            clearButton.apply {
                visibility = if (query.isBlank()) View.GONE else View.VISIBLE
                setOnClickListener { searchView.text?.clear() }
            }
        }

        viewModel.liveSearchSuggestions.observe(viewLifecycleOwner) {
            updateController(it)
        }

        setupSearch()
    }

    override fun onResume() {
        super.onResume()
        if (::searchView.isInitialized) {
            searchView.showKeyboard()
        }
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
                            search(it.title)
                        }
                )
            }
        }
    }

    private fun setupSearch() {
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isNotEmpty()) {
                    query = s.toString()
                    if (query.isNotEmpty()) {
                        viewModel.observeStreamBundles(query)
                    }
                    binding.layoutToolbarSearch.clearButton.visibility = View.VISIBLE
                } else {
                    binding.layoutToolbarSearch.clearButton.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        searchView.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == KeyEvent.ACTION_DOWN
                || actionId == KeyEvent.KEYCODE_ENTER
            ) {
                query = searchView.text.toString()
                if (query.isNotEmpty()) {
                    search(query)
                    return@setOnEditorActionListener true
                }
            }
            false
        }
    }

    private fun updateQuery(query: String) {
        searchView.text = Editable.Factory.getInstance().newEditable(query)
        searchView.setSelection(query.length)
    }

    private fun search(query: String) {
        findNavController().navigate(
            SearchSuggestionFragmentDirections
                .actionSearchSuggestionFragmentToSearchResultsFragment(query)
        )
    }
}
