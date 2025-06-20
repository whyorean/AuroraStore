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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.browse
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.Event
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.model.Dash
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.databinding.FragmentDetailsMicrogBinding
import com.aurora.store.util.PackageUtil
import com.aurora.store.view.epoxy.views.EpoxyTextViewModel_
import com.aurora.store.view.epoxy.views.preference.DashViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.onboarding.MicroGViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetailsMicroGFragment : BaseFragment<FragmentDetailsMicrogBinding>() {

    val microGViewModel: MicroGViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.apply {
            title = ""
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        with(binding) {
            // RecyclerView
            epoxyRecycler.withModels {
                setFilterDuplicates(true)

                add(
                    EpoxyTextViewModel_()
                        .id("microg_title")
                        .title(getString(R.string.onboarding_title_gsf))
                        .size(32)
                        .style(R.style.AuroraTextStyle)
                )

                add(
                    EpoxyTextViewModel_()
                        .id("microg_desc")
                        .title(getString(R.string.onboarding_title_gsf_desc))
                        .size(18)
                        .style(R.style.AuroraTextStyle)
                )

                add(
                    EpoxyTextViewModel_()
                        .id("microg_desc")
                        .title(getString(R.string.onboarding_gms_missing))
                        .size(14)
                        .style(R.style.AuroraTextStyle)
                )

                add(
                    EpoxyTextViewModel_()
                        .id("microg_gms")
                        .title(getString(R.string.onboarding_gms_microg))
                        .size(14)
                        .style(R.style.AuroraTextStyle)
                )


                dashItems().forEach {
                    add(
                        DashViewModel_()
                            .id(it.id)
                            .dash(it)
                            .click { _ ->
                                requireContext().browse(it.url)
                            }
                    )
                }
            }

            checkboxAgreement.setOnCheckedChangeListener { _, value ->
                microGViewModel.markAgreement(value)
                btnMicroG.isEnabled = value
            }

            btnMicroG.setOnClickListener { microGViewModel.downloadMicroG() }
            btnSkip.setOnClickListener { findNavController().navigateUp() }
        }

        microGViewModel.download.filterNotNull().onEach {
            when (it.downloadStatus) {
                DownloadStatus.DOWNLOADING -> updateProgressBar(visible = true, it.progress)
                DownloadStatus.FAILED -> updateProgressBar(visible = false, 0)
                DownloadStatus.QUEUED -> updateProgressBar(visible = true, -1)
                DownloadStatus.COMPLETED -> updateProgressBar(visible = true, -1)
                else -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launch {
            AuroraApp.events.installerEvent.collect { onEvent(it) }
        }
    }

    private fun onEvent(event: Event) {
        when (event) {
            is InstallerEvent.Installed -> {
                if (PackageUtil.isMicroGBundleInstalled(requireContext())) {
                    markInstallationComplete()
                }
            }

            is InstallerEvent.Failed -> markInstallationFailed()
            else -> {}
        }
    }

    private fun updateProgressBar(visible: Boolean, downloadProgress: Int) {
        with(binding.progressBar) {
            if (visible) show() else hide()
            isIndeterminate = downloadProgress == -1
            progress = downloadProgress
        }
    }

    private fun markInstallationComplete() {
        with(binding) {
            with(btnMicroG) {
                isEnabled = true
                text = getString(R.string.action_finish)
                setOnClickListener { findNavController().navigateUp() }
            }
            checkboxAgreement.isEnabled = false
            progressBar.hide()
        }
    }

    private fun markInstallationFailed() {
        with(binding) {
            with(btnMicroG) {
                isEnabled = false
                text = getString(R.string.action_install)
            }
            checkboxAgreement.isChecked = false
            progressBar.hide()
        }
    }

    private fun dashItems(): List<Dash> {
        return listOf(
            Dash(
                id = 2,
                title = requireContext().getString(R.string.details_dev_website),
                subtitle = requireContext().getString(R.string.microg_website),
                icon = R.drawable.ic_network,
                url = "https://microG.org"
            ),
            Dash(
                id = 4,
                title = requireContext().getString(R.string.privacy_policy_title),
                subtitle = requireContext().getString(R.string.microg_privacy_policy),
                icon = R.drawable.ic_privacy,
                url = "https://microg.org/privacy.html"
            ),
            Dash(
                id = 5,
                title = requireContext().getString(R.string.menu_disclaimer),
                subtitle = requireContext().getString(R.string.microg_license_agreement),
                icon = R.drawable.ic_disclaimer,
                url = "https://raw.githubusercontent.com/microg/GmsCore/refs/heads/master/LICENSE"
            )
        )
    }
}
