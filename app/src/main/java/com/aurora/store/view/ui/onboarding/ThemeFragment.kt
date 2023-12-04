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
import com.aurora.store.R
import com.aurora.store.data.model.Theme
import com.aurora.store.databinding.FragmentOnboardingThemeBinding
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_THEME_ACCENT
import com.aurora.store.util.Preferences.PREFERENCE_THEME_TYPE
import com.aurora.store.util.save
import com.aurora.store.view.epoxy.views.ThemeViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class ThemeFragment : BaseFragment(R.layout.fragment_onboarding_theme) {

    private var _binding: FragmentOnboardingThemeBinding? = null
    private val binding get() = _binding!!

    private var themeId: Int = 0
    private var accentId: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingThemeBinding.bind(view)

        themeId = Preferences.getInteger(view.context, PREFERENCE_THEME_TYPE)
        accentId = Preferences.getInteger(view.context, PREFERENCE_THEME_ACCENT)

        // RecyclerView
        binding.epoxyRecycler.withModels {
            setFilterDuplicates(true)
            loadThemesFromAssets(view.context).forEach {
                add(
                    ThemeViewModel_()
                        .id(it.id)
                        .theme(it)
                        .markChecked(themeId == it.id)
                        .checked { _, checked ->
                            if (checked) {
                                themeId = it.id
                                update(themeId)
                                requestModelBuild()
                            }
                        }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun update(themeId: Int) {
        requireActivity().recreate()
        save(PREFERENCE_THEME_TYPE, themeId)
    }

    private fun loadThemesFromAssets(context: Context): List<Theme> {
        val inputStream = context.assets.open("themes.json")
        val bytes = ByteArray(inputStream.available())
        inputStream.read(bytes)
        inputStream.close()

        val json = String(bytes, StandardCharsets.UTF_8)
        return gson.fromJson<MutableList<Theme>?>(
            json,
            object : TypeToken<MutableList<Theme?>?>() {}.type
        )
    }
}
