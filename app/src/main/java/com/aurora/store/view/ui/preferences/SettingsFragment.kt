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

package com.aurora.store.view.ui.preferences

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aurora.extensions.getStyledAttributeColor
import com.aurora.extensions.restartApp
import com.aurora.store.R
import com.aurora.store.databinding.FragmentSettingBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment(R.layout.fragment_setting),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private var _binding: FragmentSettingBinding? = null
    private val binding: FragmentSettingBinding
        get() = _binding!!

    companion object {
        var shouldRestart = false
        const val titleTag = "titleTag"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentSettingBinding.bind(view)

        if (savedInstanceState == null) {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.settings, MainPreference())
                .commit()
        } else {
            binding.layoutToolbarAction.toolbar.title = savedInstanceState.getCharSequence(titleTag)
        }

        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                binding.layoutToolbarAction.toolbar.title = getString(R.string.title_settings)
                if (shouldRestart) askRestart()
            }
        }

        // Toolbar
        binding.layoutToolbarAction.toolbar.apply {
            elevation = 0f
            title = getString(R.string.title_settings)
            navigationIcon = ContextCompat.getDrawable(view.context, R.drawable.ic_arrow_back)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(titleTag, binding.layoutToolbarAction.toolbar.title)
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        preference: Preference
    ): Boolean {
        with(childFragmentManager) {
            val args = preference.extras

            val fragment = fragmentFactory.instantiate(
                this@SettingsFragment.javaClass.classLoader!!,
                preference.fragment.toString()
            ).apply {
                arguments = args
                setTargetFragment(caller, 0)
            }

            beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(preference.key)
                .commit()

            binding.layoutToolbarAction.toolbar.title = preference.title
        }

        return true
    }

    private fun askRestart() {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.action_restart))
            .setMessage(getString(R.string.pref_dialog_to_apply_restart))
            .setPositiveButton(getString(R.string.action_restart)) { _, _ ->
                shouldRestart = false
                requireContext().restartApp()
            }
            .setNegativeButton(getString(R.string.action_later)) { dialog, _ -> dialog.dismiss() }
        val backGroundColor =
            requireContext().getStyledAttributeColor(android.R.attr.colorBackground)
        builder.background = ColorDrawable(backGroundColor)
        builder.create()
        builder.show()
    }
}
