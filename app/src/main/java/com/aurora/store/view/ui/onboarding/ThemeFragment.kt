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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import com.aurora.extensions.hide
import com.aurora.extensions.isVisible
import com.aurora.extensions.show
import com.aurora.store.R
import com.aurora.store.data.model.Theme
import com.aurora.store.databinding.FragmentOnboardingThemeBinding
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_THEME_ACCENT
import com.aurora.store.util.Preferences.PREFERENCE_THEME_TYPE
import com.aurora.store.util.save
import com.aurora.store.view.custom.CubicBezierInterpolator
import com.aurora.store.view.epoxy.views.ThemeViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.google.gson.reflect.TypeToken
import java.nio.charset.StandardCharsets
import kotlin.math.sqrt


class ThemeFragment : BaseFragment(R.layout.fragment_onboarding_theme) {

    private var _binding: FragmentOnboardingThemeBinding? = null
    private val binding get() = _binding!!

    private var themeId: Int = 0
    private var accentId: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingThemeBinding.bind(view)

        themeId = Preferences.getInteger(requireContext(), PREFERENCE_THEME_TYPE)
        accentId = Preferences.getInteger(requireContext(), PREFERENCE_THEME_ACCENT)

        // RecyclerView
        binding.epoxyRecycler.withModels {
            setFilterDuplicates(true)
            loadThemesFromAssets().forEach {
                add(
                    ThemeViewModel_()
                        .id(it.id)
                        .theme(it)
                        .markChecked(themeId == it.id)
                        .checked { v, checked ->
                            if (checked) {
                                themeId = it.id
                                update(themeId)
                                requestModelBuild()
                                animate(v)
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

    private fun animate(view: View) {
        if (binding.themeSwitchImage.isVisible()) {
            return;
        }
        try {
            val pos = IntArray(2)
            view.getLocationInWindow(pos)
            val w: Int = binding.root.measuredWidth
            val h: Int = binding.root.measuredHeight

            val bitmap = Bitmap.createBitmap(
                binding.root.measuredWidth,
                binding.root.measuredHeight,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)
            binding.root.draw(canvas)
            binding.themeSwitchImage.setImageBitmap(bitmap)
            binding.themeSwitchImage.show()

            val finalRadius = sqrt(
                ((w - pos[0]) * (w - pos[0]) + (h - pos[1]) * (h - pos[1])).toDouble()
            ).coerceAtLeast(
                sqrt((pos[0] * pos[0] + (h - pos[1]) * (h - pos[1])).toDouble())
            ).toFloat()

            val anim: Animator = ViewAnimationUtils.createCircularReveal(
                binding.root,
                pos[0],
                pos[1],
                0f,
                finalRadius
            )

            anim.duration = 450
            anim.interpolator = CubicBezierInterpolator.EASE_IN_OUT_QUAD
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.themeSwitchImage.setImageDrawable(null)
                    binding.themeSwitchImage.hide()
                }
            })
            anim.start()
        } catch (ignore: Throwable) {
        }
    }

    private fun loadThemesFromAssets(): List<Theme> {
        val inputStream = requireContext().assets.open("themes.json")
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
