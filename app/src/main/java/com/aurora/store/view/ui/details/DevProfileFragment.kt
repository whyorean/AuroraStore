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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.data.models.details.DevStream
import com.aurora.store.R
import com.aurora.store.data.ViewState
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.FragmentDevProfileBinding
import com.aurora.store.view.epoxy.controller.DeveloperCarouselController
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.details.DevProfileViewModel

class DevProfileFragment : BaseFragment(R.layout.fragment_dev_profile),
    GenericCarouselController.Callbacks {

    private var _binding: FragmentDevProfileBinding? = null
    private val binding: FragmentDevProfileBinding
        get() = _binding!!

    private val args: DevProfileFragmentArgs by navArgs()

    private lateinit var VM: DevProfileViewModel
    private lateinit var C: DeveloperCarouselController
    private lateinit var authData: AuthData

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDevProfileBinding.bind(view)

        C = DeveloperCarouselController(this)
        VM = ViewModelProvider(this)[DevProfileViewModel::class.java]
        authData = AuthProvider.with(requireContext()).getAuthData()

        // Toolbar
        binding.layoutToolbarAction.apply {
            txtTitle.text =
                if (args.title.isNullOrBlank()) getString(R.string.details_dev_profile) else args.title
            toolbar.setOnClickListener { findNavController().navigateUp() }
        }

        // RecyclerView
        binding.recycler.setController(C)

        VM.liveData.observe(viewLifecycleOwner) {
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
                        binding.layoutToolbarAction.txtTitle.text = title
                        binding.txtDevName.text = title
                        binding.txtDevDescription.text = description
                        binding.imgIcon.load(imgUrl)
                        binding.viewFlipper.displayedChild = 0
                        C.setData(streamBundle)
                    }
                }
            }
        }

        binding.viewFlipper.displayedChild = 1
        VM.getStreamBundle(args.devId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {
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
