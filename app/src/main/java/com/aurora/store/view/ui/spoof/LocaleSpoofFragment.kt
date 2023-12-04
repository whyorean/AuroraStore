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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.data.providers.SpoofProvider
import com.aurora.store.databinding.FragmentGenericRecyclerBinding
import com.aurora.store.view.epoxy.views.preference.LocaleViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class LocaleSpoofFragment : BaseFragment() {

    private lateinit var B: FragmentGenericRecyclerBinding
    private lateinit var spoofProvider: SpoofProvider

    private var locale: Locale = Locale.getDefault()

    private val TAG = LocaleSpoofFragment::class.java.simpleName

    companion object {
        @JvmStatic
        fun newInstance(): LocaleSpoofFragment {
            return LocaleSpoofFragment().apply {

            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        B = FragmentGenericRecyclerBinding.bind(
            inflater.inflate(
                R.layout.fragment_generic_recycler,
                container,
                false
            )
        )

        spoofProvider = SpoofProvider(requireContext())

        return B.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (spoofProvider.isLocaleSpoofEnabled())
            locale = spoofProvider.getSpoofLocale()

        try {
            updateController(fetchAvailableLocales())
        } catch (exception: Exception) {
            Log.e(TAG, "Could not get available locales", exception)
        }
    }

    private fun updateController(locales: List<Locale>) {
        B.recycler.withModels {
            setFilterDuplicates(true)
            locales
                .sortedBy { it.displayName }
                .forEach {
                add(
                    LocaleViewModel_()
                        .id(it.language)
                        .markChecked(locale == it)
                        .checked { _, checked ->
                            if (checked) {
                                locale = it
                                saveSelection(it)
                                requestModelBuild()
                            }
                        }
                        .locale(it)
                )
            }
        }
    }

    private fun fetchAvailableLocales(): List<Locale> {
        val locales = Locale.getAvailableLocales()
        val localeList: MutableList<Locale> = ArrayList()
        localeList.addAll(locales)
        localeList.add(0, Locale.getDefault())
        return localeList
    }

    private fun saveSelection(locale: Locale) {
        requireContext().toast(R.string.spoof_apply)
        spoofProvider.setSpoofLocale(locale)
    }
}
