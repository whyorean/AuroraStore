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

package com.aurora.store.view.ui.about

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.aurora.extensions.browse
import com.aurora.extensions.copyToClipBoard
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.model.Link
import com.aurora.store.databinding.FragmentAboutBinding
import com.aurora.store.view.epoxy.views.preference.LinkViewModel_

class AboutFragment : Fragment(R.layout.fragment_about) {

    private var _binding: FragmentAboutBinding? = null
    private val binding: FragmentAboutBinding
        get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAboutBinding.bind(view)

        // Toolbar
        binding.layoutToolbarAction.txtTitle.text = getString(R.string.title_about)
        binding.layoutToolbarAction.imgActionPrimary.setOnClickListener {
            findNavController().navigateUp()
        }

        // About Details
        binding.imgIcon.load(R.mipmap.ic_launcher)
        binding.line2.text = ("v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")

        binding.epoxyRecycler.layoutManager =
            LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        updateController()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateController() {
        val linkURLS = resources.getStringArray(R.array.link_urls)
        val linkTitles = resources.getStringArray(R.array.link_titles)
        val linkSummary = resources.getStringArray(R.array.link_subtitle)

        val linkIcons = intArrayOf(
            R.drawable.ic_bitcoin_btc,
            R.drawable.ic_bitcoin_bch,
            R.drawable.ic_ethereum_eth,
            R.drawable.ic_bhim,
            R.drawable.ic_paypal,
            R.drawable.ic_libera_pay,
            R.drawable.ic_gitlab,
            R.drawable.ic_xda,
            R.drawable.ic_telegram,
            R.drawable.ic_fdroid
        )

        binding.epoxyRecycler.withModels {
            for (i in linkURLS.indices) {
                val link = Link(
                    id = i,
                    title = linkTitles[i],
                    subtitle = linkSummary[i],
                    url = linkURLS[i],
                    icon = linkIcons[i]
                )
                add(
                    LinkViewModel_()
                        .id(i)
                        .link(link)
                        .click { _ -> processUrl(link.url) }
                )
            }
        }
    }

    private fun processUrl(url: String) {
        when {
            url.startsWith("http") -> context?.browse(url)
            url.startsWith("upi") -> context?.browse(url)
            else -> context?.copyToClipBoard(url)
        }
    }
}
