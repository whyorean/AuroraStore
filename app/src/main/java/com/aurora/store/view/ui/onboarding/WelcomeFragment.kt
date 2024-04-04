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

import android.os.Bundle
import android.view.View
import com.aurora.extensions.browse
import com.aurora.store.R
import com.aurora.store.data.model.Dash
import com.aurora.store.databinding.FragmentOnboardingWelcomeBinding
import com.aurora.store.view.epoxy.views.preference.DashViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeFragment : BaseFragment(R.layout.fragment_onboarding_welcome) {

    private var _binding: FragmentOnboardingWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingWelcomeBinding.bind(view)

        // RecyclerView
        binding.epoxyRecycler.withModels {
            setFilterDuplicates(true)
            loadDashFromAssets().forEach {
                add(
                    DashViewModel_()
                        .id(it.id)
                        .dash(it)
                        .click { _ -> requireContext().browse(it.url) }
                )
            }
        }
    }

    private fun loadDashFromAssets(): List<Dash> {
        return listOf(
            Dash(
                id = 0,
                title = requireContext().getString(R.string.faqs_title),
                subtitle = requireContext().getString(R.string.faqs_subtitle),
                icon = R.drawable.ic_faq,
                url = "https://gitlab.com/AuroraOSS/AuroraStore/-/wikis/Frequently%20Asked%20Questions"
            ),
            Dash(
                id = 1,
                title = requireContext().getString(R.string.source_code_title),
                subtitle = requireContext().getString(R.string.source_code_subtitle),
                icon = R.drawable.ic_code,
                url = "https://gitlab.com/AuroraOSS/AuroraStore/"
            ),
            Dash(
                id = 2,
                title = requireContext().getString(R.string.menu_license),
                subtitle = requireContext().getString(R.string.license_subtitle),
                icon = R.drawable.ic_license,
                url = "https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/LICENSE"
            ),
            Dash(
                id = 3,
                title = requireContext().getString(R.string.privacy_policy_title),
                subtitle = requireContext().getString(R.string.privacy_policy_subtitle),
                icon = R.drawable.ic_privacy,
                url = "https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/POLICY.md"
            ),
            Dash(
                id = 4,
                title = requireContext().getString(R.string.menu_disclaimer),
                subtitle = requireContext().getString(R.string.disclaimer_subtitle),
                icon = R.drawable.ic_disclaimer,
                url = "https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/DISCLAIMER.md"
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
