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

package com.aurora.store.view.ui.spoof

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.databinding.FragmentGenericRecyclerBinding
import com.aurora.store.view.epoxy.views.TextDividerViewModel_
import com.aurora.store.view.epoxy.views.preference.LocaleViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.spoof.SpoofViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class LocaleSpoofFragment : BaseFragment<FragmentGenericRecyclerBinding>() {

    private val viewModel: SpoofViewModel by viewModels()

    companion object {
        @JvmStatic
        fun newInstance(): LocaleSpoofFragment {
            return LocaleSpoofFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.availableLocales.collect {
                updateController(it)
            }
        }
    }

    private fun updateController(locales: List<Locale>) {
        binding.recycler.withModels {
            setFilterDuplicates(true)

            add(
                TextDividerViewModel_()
                    .id("default_divider")
                    .title(getString(R.string.default_spoof))
            )

            add(
                LocaleViewModel_()
                    .id(viewModel.defaultLocale.language)
                    .markChecked(viewModel.isLocaleSelected(viewModel.defaultLocale))
                    .checked { _, checked ->
                        if (checked) {
                            viewModel.onLocaleSelected(viewModel.defaultLocale)
                            requestModelBuild()
                            findNavController().navigate(R.id.forceRestartDialog)
                        }
                    }
                    .locale(viewModel.defaultLocale)
            )

            add(
                TextDividerViewModel_()
                    .id("available_divider")
                    .title(getString(R.string.available_spoof))
            )

            locales.forEach {
                add(
                    LocaleViewModel_()
                        .id(it.language)
                        .markChecked(viewModel.spoofProvider.locale == it)
                        .checked { _, checked ->
                            if (checked) {
                                viewModel.onLocaleSelected(it)
                                requestModelBuild()
                                findNavController().navigate(R.id.forceRestartDialog)
                            }
                        }
                        .locale(it)
                )
            }
        }
    }
}
