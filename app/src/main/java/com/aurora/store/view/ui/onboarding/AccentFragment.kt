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

package com.aurora.store.view.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aurora.extensions.isSAndAbove
import com.aurora.store.R
import com.aurora.store.data.model.Accent
import com.aurora.store.databinding.FragmentOnboardingAccentBinding
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_THEME_ACCENT
import com.aurora.store.util.Preferences.PREFERENCE_THEME_TYPE
import com.aurora.store.util.save
import com.aurora.store.view.epoxy.views.AccentViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.google.gson.reflect.TypeToken
import java.nio.charset.StandardCharsets


class AccentFragment : BaseFragment(R.layout.fragment_onboarding_accent) {

    private var _binding: FragmentOnboardingAccentBinding? = null
    private val binding get() = _binding!!

    private var themeId: Int = 0
    private var accentId: Int = if (isSAndAbove()) 0 else 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingAccentBinding.bind(view)

        themeId = Preferences.getInteger(view.context, PREFERENCE_THEME_TYPE)
        accentId = Preferences.getInteger(view.context, PREFERENCE_THEME_ACCENT)

        // RecyclerView
        with(binding.epoxyRecycler) {
            setHasFixedSize(true)
            layoutManager = StaggeredGridLayoutManager(5, StaggeredGridLayoutManager.VERTICAL)
        }

        binding.epoxyRecycler.withModels {
            setFilterDuplicates(true)

            loadAccentsFromAssets(view.context).forEach {
                if (it.id == 0) {
                    if (!isSAndAbove()) return@forEach
                }

                add(
                    AccentViewModel_()
                        .id(it.id)
                        .accent(it)
                        .markChecked(accentId == it.id)
                        .click { _ ->
                            accentId = it.id
                            updateAccent(accentId)
                            requestModelBuild()
                        }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateAccent(accentId: Int) {
        requireActivity().recreate()
        save(PREFERENCE_THEME_ACCENT, accentId)
    }

    private fun loadAccentsFromAssets(context: Context): List<Accent> {
        val inputStream = context.assets.open("accent.json")
        val bytes = ByteArray(inputStream.available())
        inputStream.read(bytes)
        inputStream.close()

        val json = String(bytes, StandardCharsets.UTF_8)
        return gson.fromJson<MutableList<Accent>?>(
            json,
            object : TypeToken<MutableList<Accent?>?>() {}.type
        )
    }
}
