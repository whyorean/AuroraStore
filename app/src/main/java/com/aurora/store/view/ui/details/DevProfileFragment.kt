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

package com.aurora.store.view.ui.details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil3.load
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.data.models.details.DevStream
import com.aurora.store.R
import com.aurora.store.data.model.ViewState
import com.aurora.store.databinding.FragmentDevProfileBinding
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.details.DevProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DevProfileFragment : BaseFragment<FragmentDevProfileBinding>(),
    GenericCarouselController.Callbacks {

    private val args: DevProfileFragmentArgs by navArgs()
    private val viewModel: DevProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Toolbar
        binding.toolbar.apply {
            title = if (args.title.isNullOrBlank()) getString(R.string.details_dev_profile) else args.title
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        // RecyclerView

        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is ViewState.Empty -> {
                }

                is ViewState.Loading -> {

                }

                is ViewState.Error -> {

                }

                is ViewState.Status -> {

                }

                is ViewState.Success<*> -> {
                    (it.data as DevStream).apply {
                        binding.toolbar.title = title
                        binding.txtDevName.text = title
                        binding.txtDevDescription.text = description
                        binding.imgIcon.load(imgUrl)
                        binding.viewFlipper.displayedChild = 0
                    }
                }
            }
        }

        binding.viewFlipper.displayedChild = 1
        viewModel.getStreamBundle(args.devId)
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {
        openStreamBrowseFragment(streamCluster.clusterBrowseUrl, streamCluster.clusterTitle)
    }

    override fun onClusterScrolled(streamCluster: StreamCluster) {
        viewModel.observeCluster(streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsFragment(app.packageName, app)
    }

    override fun onAppLongClick(app: App) {

    }
}
