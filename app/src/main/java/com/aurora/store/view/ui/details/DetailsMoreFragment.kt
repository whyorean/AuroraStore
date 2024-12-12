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
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.databinding.FragmentDetailsMoreBinding
import com.aurora.store.view.epoxy.views.HeaderViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppAltViewModel_
import com.aurora.store.view.epoxy.views.details.AppDependentViewModel_
import com.aurora.store.view.epoxy.views.details.FileViewModel_
import com.aurora.store.view.epoxy.views.details.InfoViewModel_
import com.aurora.store.view.epoxy.views.details.MoreBadgeViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.details.DetailsMoreViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID

@AndroidEntryPoint
class DetailsMoreFragment : BaseFragment<FragmentDetailsMoreBinding>() {

    private val viewModel: DetailsMoreViewModel by viewModels()
    private val args: DetailsMoreFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adjust layout for edgeToEdge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerMore) { layout, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            layout.setPadding(0, 0, 0, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // Toolbar
        binding.toolbar.setOnClickListener { findNavController().navigateUp() }

        inflateDescription(args.app)
        inflateFiles(args.app)
        viewModel.fetchDependentApps(args.app)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dependentApps.collect { list ->
                if (list.isNotEmpty()) {
                    binding.recyclerDependency.withModels {
                        list.filter { it.displayName.isNotEmpty() }.forEach {
                            add(
                                AppDependentViewModel_()
                                    .id(it.id)
                                    .app(it)
                                    .click { _ -> openDetailsFragment(it.packageName, it) }
                            )
                        }
                    }
                } else {
                    binding.recyclerDependency.withModels {
                        add(
                            NoAppAltViewModel_()
                                .id("no_app")
                                .message(getString(R.string.details_no_dependencies))
                        )
                    }
                }
            }
        }
    }

    private fun inflateDescription(app: App) {
        binding.toolbar.title = app.displayName
        binding.txtDescription.text = HtmlCompat.fromHtml(
            app.description,
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    }

    private fun inflateFiles(app: App) {
        binding.recyclerMore.withModels {
            //Add dependent files
            if (app.fileList.isNotEmpty()) {
                add(
                    HeaderViewModel_()
                        .id("badge_header")
                        .title("Files")
                )

                app.fileList.forEach {
                    add(
                        FileViewModel_()
                            .id(it.id)
                            .file(it)
                    )
                }
            }

            //Add display & extra badges
            if (app.infoBadges.isNotEmpty()) {
                add(
                    HeaderViewModel_()
                        .id("badge_header")
                        .title("More")
                )

                app.infoBadges.forEach {
                    add(
                        MoreBadgeViewModel_()
                            .id(it.id)
                            .badge(it)
                    )
                }

                if (app.displayBadges.isNotEmpty()) {
                    app.displayBadges
                        .filter { it.textMajor.isNotEmpty() }
                        .forEach {
                            add(
                                MoreBadgeViewModel_()
                                    .id(it.id)
                                    .badge(it)
                            )
                        }
                }
            }

            if (app.appInfo.appInfoMap.isNotEmpty()) {
                add(
                    HeaderViewModel_()
                        .id("info_header")
                        .title("Info")
                )
                app.appInfo.appInfoMap.forEach {
                    add(
                        InfoViewModel_()
                            .id(it.key)
                            .badge(it)
                    )
                }
                add(
                    InfoViewModel_()
                        .id(UUID.randomUUID().toString())
                        .badge(mapOf("targets" to "API ${app.targetSdk}").entries.first())
                )
            }
        }
    }
}
