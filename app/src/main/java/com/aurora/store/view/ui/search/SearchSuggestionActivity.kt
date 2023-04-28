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

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.aurora.Constants
import com.aurora.extensions.close
import com.aurora.extensions.getEmptyActivityBundle
import com.aurora.extensions.open
import com.aurora.extensions.showKeyboard
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.store.databinding.ActivitySearchSuggestionBinding
import com.aurora.store.view.epoxy.views.SearchSuggestionViewModel_
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.view.ui.downloads.DownloadActivity
import com.aurora.store.viewmodel.search.SearchSuggestionViewModel
import com.google.android.material.textfield.TextInputEditText


class SearchSuggestionActivity : BaseActivity() {

    lateinit var B: ActivitySearchSuggestionBinding
    lateinit var VM: SearchSuggestionViewModel

    lateinit var searchView: TextInputEditText

    var query: String = String()

    override fun onConnected() {
        hideNetworkConnectivitySheet()
    }

    override fun onDisconnected() {
        showNetworkConnectivitySheet()
    }

    override fun onReconnected() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        B = ActivitySearchSuggestionBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this)[SearchSuggestionViewModel::class.java]

        setContentView(B.root)

        attachToolbar()

        VM.liveSearchSuggestions.observe(this, {
            updateController(it)
        })

        setupSearch()
    }

    override fun onResume() {
        super.onResume()
        if (::searchView.isInitialized) {
            searchView.showKeyboard()
        }
    }

    private fun attachToolbar() {
        searchView = B.layoutToolbarSearch.inputSearch

        B.layoutToolbarSearch.imgActionPrimary.setOnClickListener {
            close()
        }
        B.layoutToolbarSearch.imgActionSecondary.setOnClickListener {
            open(DownloadActivity::class.java)
        }
    }

    private fun updateController(searchSuggestions: List<SearchSuggestEntry>) {
        B.recycler.withModels {
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
                        VM.observeStreamBundles(query)
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        searchView.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
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
        val intent = Intent(this, SearchResultsActivity::class.java)
        intent.putExtra(Constants.STRING_EXTRA, query)
        startActivity(intent, getEmptyActivityBundle())
    }
}
