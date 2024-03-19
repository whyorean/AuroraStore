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

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.SearchBundle
import com.aurora.store.R
import com.aurora.store.data.Filter
import com.aurora.store.data.providers.FilterProvider
import com.aurora.store.databinding.FragmentSearchResultBinding
import com.aurora.store.util.Preferences
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.search.SearchResultViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchResultsFragment : BaseFragment(R.layout.fragment_search_result),
    OnSharedPreferenceChangeListener {

    private var _binding: FragmentSearchResultBinding? = null
    private val binding: FragmentSearchResultBinding
        get() = _binding!!

    lateinit var VM: SearchResultViewModel

    lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener
    lateinit var searchView: TextInputEditText

    lateinit var sharedPreferences: SharedPreferences
    lateinit var filter: Filter

    var query: String? = null
    var searchBundle: SearchBundle = SearchBundle()

    private var shimmerAnimationVisible = false
    private var snackbar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentSearchResultBinding.bind(view)
        VM = ViewModelProvider(this)[SearchResultViewModel::class.java]

        sharedPreferences = Preferences.getPrefs(view.context)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        filter = FilterProvider.with(view.context).getSavedFilter()

        // Toolbar
        binding.layoutViewToolbar.apply {
            searchView = inputSearch
            imgActionPrimary.setOnClickListener {
                findNavController().navigateUp()
            }
            imgActionSecondary.setOnClickListener {
                findNavController().navigate(R.id.downloadFragment)
            }
        }

        // Search
        attachSearch()

        // RecyclerView
        endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                VM.next(searchBundle.subBundles)
            }
        }
        binding.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)

        // Filter
        binding.filterFab.setOnClickListener {
            findNavController().navigate(R.id.filterSheet)
        }

        VM.liveData.observe(viewLifecycleOwner) {
            if (shimmerAnimationVisible) {
                endlessRecyclerOnScrollListener.resetPageCount()
                binding.recycler.clear()
                shimmerAnimationVisible = false
            }
            searchBundle = it
            updateController(searchBundle)
        }

        query = requireArguments().getString("query")
        query?.let { updateQuery(it) }
    }

    override fun onDestroyView() {
        context?.let {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            if (Preferences.getBoolean(it, Preferences.PREFERENCE_FILTER_SEARCH)) {
                FilterProvider.with(it).saveFilter(Filter())
            }
        }
        super.onDestroyView()
        _binding = null
    }

    private fun updateController(searchBundle: SearchBundle?) {
        if (searchBundle == null) {
            shimmerAnimationVisible = true
            binding.recycler.withModels {
                for (i in 1..10) {
                    add(AppListViewShimmerModel_().id(i))
                }
            }
            return
        }

        val filteredAppList = filter(searchBundle.appList)

        if (filteredAppList.isEmpty()) {
            if (searchBundle.subBundles.isNotEmpty()) {
                VM.next(searchBundle.subBundles)
                binding.recycler.withModels {
                    setFilterDuplicates(true)
                    add(
                        AppProgressViewModel_()
                            .id("progress")
                    )
                }
            } else {
                binding.recycler.adapter?.let {
                    /*Show empty search list if nothing found or no app matches filter criterion*/
                    if (it.itemCount == 1 && searchBundle.subBundles.isEmpty()) {
                        binding.recycler.withModels {
                            add(
                                NoAppViewModel_()
                                    .id("no_app")
                                    .message(getString(R.string.details_no_app_match))
                                    .icon(R.drawable.ic_round_search)
                            )
                        }
                    }
                }
            }
        } else {
            binding.recycler
                .withModels {
                    setFilterDuplicates(true)

                    filteredAppList.forEach { app ->
                        add(
                            AppListViewModel_()
                                .id(app.id)
                                .app(app)
                                .click(View.OnClickListener {
                                    openDetailsFragment(app.packageName, app)
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

            binding.recycler.adapter?.let {
                if (it.itemCount < 10) {
                    VM.next(searchBundle.subBundles)
                }
            }
        }
    }

    private fun attachSearch() {
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {}
        })

        searchView.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == KeyEvent.ACTION_DOWN
                || actionId == KeyEvent.KEYCODE_ENTER
            ) {
                query = searchView.text.toString()
                query?.let {
                    requireArguments().putString("query", it)
                    queryViewModel(it)
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
        updateController(null)
        VM.observeSearchResults(query)
    }

    private fun filter(appList: MutableList<App>): List<App> {
        val tempList:MutableList<App> = mutableListOf()
        tempList.addAll(appList)

        filter = FilterProvider.with(requireContext()).getSavedFilter()
        return tempList
            .asSequence()
            .filter { it.displayName.isNotEmpty() } // Some of the apps may not have metadata
            .filter { if (!filter.paidApps) it.isFree else true }
            .filter { if (!filter.appsWithAds) !it.containsAds else true }
            .filter { if (!filter.gsfDependentApps) it.dependencies.dependentPackages.isEmpty() else true }
            .filter { if (filter.rating > 0) it.rating.average >= filter.rating else true }
            .filter { if (filter.downloads > 0) it.installs >= filter.downloads else true }
            .toList()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == FilterProvider.PREFERENCE_FILTER) {
            filter = FilterProvider.with(requireContext()).getSavedFilter()
            query?.let {
                queryViewModel(it)
            }
        }
    }
}
