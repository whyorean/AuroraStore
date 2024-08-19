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
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.data.providers.NativeDeviceInfoProvider
import com.aurora.store.data.providers.SpoofProvider
import com.aurora.store.databinding.FragmentGenericRecyclerBinding
import com.aurora.store.view.epoxy.views.preference.DeviceViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.spoof.SpoofViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Properties
import javax.inject.Inject

@AndroidEntryPoint
class DeviceSpoofFragment : BaseFragment<FragmentGenericRecyclerBinding>() {
    private val viewModel: SpoofViewModel by viewModels()

    @Inject
    lateinit var spoofProvider: SpoofProvider

    private lateinit var properties: Properties

    companion object {
        @JvmStatic
        fun newInstance(): DeviceSpoofFragment {
            return DeviceSpoofFragment().apply {

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        properties = if (spoofProvider.isDeviceSpoofEnabled()) {
            spoofProvider.getSpoofDeviceProperties()
        } else {
            NativeDeviceInfoProvider(requireContext()).getNativeDeviceProperties()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.availableDevices.collect { updateController(it) }
            }
        }
        viewModel.fetchAvailableDevices()
    }

    private fun updateController(locales: List<Properties>) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            locales
                .sortedBy { it.getProperty("UserReadableName") }
                .forEach {
                    add(
                        DeviceViewModel_()
                            .id(it.hashCode())
                            .markChecked(
                                properties.getProperty("UserReadableName") == it.getProperty(
                                    "UserReadableName"
                                )
                            )
                            .checked { _, checked ->
                                if (checked) {
                                    properties = it
                                    saveSelection(it)
                                    requestModelBuild()
                                }
                            }
                            .properties(it)
                    )
                }
        }
    }

    private fun saveSelection(properties: Properties) {
        requireContext().toast(R.string.spoof_apply)
        spoofProvider.setSpoofDeviceProperties(properties)
    }
}
