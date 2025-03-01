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
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aurora.store.R
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.databinding.FragmentGenericRecyclerBinding
import com.aurora.store.view.epoxy.views.TextDividerViewModel_
import com.aurora.store.view.epoxy.views.preference.DeviceViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.spoof.SpoofViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Properties

@AndroidEntryPoint
class DeviceSpoofFragment : BaseFragment<FragmentGenericRecyclerBinding>() {

    private val viewModel: SpoofViewModel by viewModels()

    companion object {
        @JvmStatic
        fun newInstance(): DeviceSpoofFragment {
            return DeviceSpoofFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.availableDevices.collect { updateController(it) }
            }
        }
    }

    private fun updateController(devices: List<Properties>) {
        binding.recycler.withModels {
            setFilterDuplicates(true)

            add(
                TextDividerViewModel_()
                    .id("default_divider")
                    .title(getString(R.string.default_spoof))
            )

            add(
                DeviceViewModel_()
                    .id(viewModel.defaultProperties.hashCode())
                    .markChecked(viewModel.isDeviceSelected(viewModel.defaultProperties))
                    .checked { _, checked ->
                        if (checked) {
                            viewModel.onDeviceSelected(viewModel.defaultProperties)
                            requestModelBuild()
                            AccountProvider.logout(requireContext())
                            findNavController().navigate(R.id.forceRestartDialog)
                        }
                    }
                    .properties(viewModel.defaultProperties)
            )

            add(
                TextDividerViewModel_()
                    .id("available_divider")
                    .title(getString(R.string.available_spoof))
            )

            devices.forEach {
                add(
                    DeviceViewModel_()
                        .id(it.hashCode())
                        .markChecked(viewModel.isDeviceSelected(it))
                        .checked { _, checked ->
                            if (checked) {
                                viewModel.onDeviceSelected(it)
                                requestModelBuild()
                                AccountProvider.logout(requireContext())
                                findNavController().navigate(R.id.forceRestartDialog)
                            }
                        }
                        .properties(it)
                )
            }
        }
    }
}
