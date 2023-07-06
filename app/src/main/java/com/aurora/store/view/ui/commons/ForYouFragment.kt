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

package com.aurora.store.view.ui.commons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.R
import com.aurora.store.data.ViewState
import com.aurora.store.databinding.FragmentForYouBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.viewmodel.homestream.AppsForYouViewModel
import com.aurora.store.viewmodel.homestream.BaseClusterViewModel
import com.aurora.store.viewmodel.homestream.GamesForYouViewModel


class ForYouFragment : BaseFragment(), GenericCarouselController.Callbacks {

    private lateinit var B: FragmentForYouBinding
    private lateinit var C: GenericCarouselController
    private lateinit var VM: BaseClusterViewModel

    private lateinit var streamBundle: StreamBundle
    private lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener

    private var pageType = 0

    companion object {
        @JvmStatic
        fun newInstance(pageType: Int): ForYouFragment {
            return ForYouFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.PAGE_TYPE, pageType)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        B = FragmentForYouBinding.bind(
            inflater.inflate(
                R.layout.fragment_for_you,
                container,
                false
            )
        )

        C = GenericCarouselController(this)

        val bundle = arguments
        if (bundle != null) {
            pageType = bundle.getInt(Constants.PAGE_TYPE, 0)
        }

        when (pageType) {
            0 -> VM =
                ViewModelProvider(requireActivity()).get(AppsForYouViewModel::class.java)
            1 -> VM =
                ViewModelProvider(requireActivity()).get(GamesForYouViewModel::class.java)
        }

        return B.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        B.recycler.setController(C)

        VM.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is ViewState.Empty -> {
                }
                is ViewState.Loading -> {
                    updateController(null)
                }
                is ViewState.Error -> {

                }
                is ViewState.Status -> {

                }
                is ViewState.Success<*> -> {
                    if (!::streamBundle.isInitialized)
                        attachRecycler()

                    streamBundle = it.data as StreamBundle

                    updateController(streamBundle)
                }
            }
        }
    }

    private fun attachRecycler() {
        endlessRecyclerOnScrollListener =
            object : EndlessRecyclerOnScrollListener() {
                override fun onLoadMore(currentPage: Int) {
                    VM.observe()
                }
            }

        B.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
    }

    private fun updateController(streamBundle: StreamBundle?) {
        C.setData(streamBundle)
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {
        if (streamCluster.clusterBrowseUrl.isNotEmpty())
            openStreamBrowseFragment(streamCluster.clusterBrowseUrl, streamCluster.clusterTitle)
    }

    override fun onClusterScrolled(streamCluster: StreamCluster) {
        VM.observeCluster(streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsFragment(app)
    }

    override fun onAppLongClick(app: App) {

    }
}
