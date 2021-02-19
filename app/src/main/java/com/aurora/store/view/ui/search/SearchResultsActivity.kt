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
import androidx.lifecycle.ViewModelProvider
import com.aurora.Constants
import com.aurora.gplayapi.data.models.SearchBundle
import com.aurora.store.databinding.ActivitySearchResultBinding
import com.aurora.store.util.extensions.close
import com.aurora.store.util.extensions.open
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.view.ui.downloads.DownloadActivity
import com.aurora.store.viewmodel.search.SearchResultViewModel
import com.google.android.material.textfield.TextInputEditText


class SearchResultsActivity : BaseActivity() {

    lateinit var B: ActivitySearchResultBinding
    lateinit var VM: SearchResultViewModel

    lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener
    lateinit var searchView: TextInputEditText

    var query: String? = null
    var searchBundle: SearchBundle = SearchBundle()

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

        B = ActivitySearchResultBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this).get(SearchResultViewModel::class.java)

        setContentView(B.root)

        VM.liveData.observe(this, {
            searchBundle = it
            updateController(searchBundle)
        })

        attachToolbar()
        attachSearch()
        attachRecycler()

        query = intent.getStringExtra(Constants.STRING_EXTRA)
        query?.let {
            updateQuery(it)
        }
    }

    private fun attachToolbar() {
        searchView = B.layoutViewToolbar.inputSearch

        B.layoutViewToolbar.imgActionPrimary.setOnClickListener {
            close()
        }
        B.layoutViewToolbar.imgActionSecondary.setOnClickListener {
            open(DownloadActivity::class.java)
        }
    }

    private fun attachRecycler() {
        endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                VM.next(searchBundle.subBundles)
            }
        }
        B.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
    }

    private fun updateController(searchBundle: SearchBundle) {
        B.recycler
            .withModels {
                setFilterDuplicates(true)
                searchBundle.appList.forEach { app ->
                    add(
                        AppListViewModel_()
                            .id(app.id)
                            .app(app)
                            .click(View.OnClickListener {
                                openDetailsActivity(app)
                            })
                    )
                }

                if (searchBundle.subBundles.isNotEmpty()) {
                    add(
                        AppProgressViewModel_()
                            .id("progress")
                    )
                }
            }
    }

    private fun attachSearch() {
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isNotEmpty()) {
                    /*val query = s.toString()
                    if (query.isNotEmpty()) {
                        VM.observeSearchResults(query)
                    }*/
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        searchView.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchView.text.toString()
                if (query.isNotEmpty()) {
                    queryViewModel(query)
                    return@setOnEditorActionListener true
                }
            }
            false
        }
    }

    private fun updateQuery(query: String) {
        searchView.text = Editable.Factory.getInstance().newEditable(query)
        searchView.setSelection(query.length)
        queryViewModel(query)
    }

    private fun queryViewModel(query: String) {
        endlessRecyclerOnScrollListener.resetPageCount()
        B.recycler.clear()
        searchBundle.subBundles.clear()
        searchBundle.appList.clear()
        VM.observeSearchResults(query)
    }
}