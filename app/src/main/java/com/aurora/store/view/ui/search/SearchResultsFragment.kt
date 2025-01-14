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
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.hideKeyboard
import com.aurora.extensions.showKeyboard
import com.aurora.store.R
import com.aurora.store.data.model.Filter
import com.aurora.store.data.providers.FilterProvider.Companion.PREFERENCE_FILTER
import com.aurora.store.databinding.FragmentSearchResultBinding
import com.aurora.store.util.Preferences
import com.aurora.store.view.epoxy.controller.AppListEpoxyController
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.search.SearchResultViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class SearchResultsFragment : BaseFragment<FragmentSearchResultBinding>(),
    OnSharedPreferenceChangeListener {

    private val viewModel: SearchResultViewModel by viewModels()

    private lateinit var sharedPreferences: SharedPreferences

    private var query: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adjust FAB margins for edgeToEdge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.filterFab) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.filterFab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin += insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }

        sharedPreferences = Preferences.getPrefs(view.context)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        // Toolbar
        binding.toolbar.apply {
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

        // Search
        attachSearch()
        query = requireArguments().getString("query")?.also {
            binding.searchBar.text = Editable.Factory.getInstance().newEditable(it)
            binding.searchBar.setSelection(it.length)
            viewModel.search(it)
        }

        // RecyclerView
        val epoxyController = AppListEpoxyController(viewLifecycleOwner) { app ->
            binding.searchBar.hideKeyboard()
            openDetailsFragment(app.packageName, app)
        }

        binding.recycler.setController(epoxyController)
        viewModel.apps.onEach {
            epoxyController.submitData(it)
        }.launchIn(lifecycleScope)

        // Filter
        binding.filterFab.setOnClickListener {
            findNavController().navigate(R.id.filterSheet)
        }
    }

    override fun onDestroyView() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
    }

    override fun onDestroy() {
        viewModel.filterProvider.saveFilter(Filter())
        super.onDestroy()
    }

    private fun attachSearch() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.toolbar.menu.findItem(R.id.action_clear)?.isVisible = s.isNotBlank()
            }

            override fun afterTextChanged(s: Editable) {}
        })

        binding.searchBar.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            when (actionId) {
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ENTER,
                EditorInfo.IME_ACTION_SEARCH -> {
                    query = binding.searchBar.text?.toString()?.also {
                        requireArguments().putString("query", it)
                        viewModel.search(it)
                        return@setOnEditorActionListener true
                    }
                }
            }
            false
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREFERENCE_FILTER) query?.let { viewModel.search(it) }
    }
}
