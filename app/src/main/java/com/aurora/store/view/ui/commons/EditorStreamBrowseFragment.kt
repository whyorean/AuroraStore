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
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyModel
import com.aurora.gplayapi.data.models.App
import com.aurora.store.databinding.FragmentGenericWithToolbarBinding
import com.aurora.store.view.epoxy.groups.CarouselHorizontalModel_
import com.aurora.store.view.epoxy.views.EditorHeadViewModel_
import com.aurora.store.view.epoxy.views.HorizontalDividerViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.details.MiniScreenshotView
import com.aurora.store.view.epoxy.views.details.MiniScreenshotViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.editorschoice.EditorBrowseViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditorStreamBrowseFragment : BaseFragment<FragmentGenericWithToolbarBinding>() {
    private val args: EditorStreamBrowseFragmentArgs by navArgs()
    private val viewModel: EditorBrowseViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.layoutToolbarAction.apply {
            txtTitle.text = args.title
            imgActionPrimary.setOnClickListener {
                findNavController().navigateUp()
            }
        }

        viewModel.liveData.observe(viewLifecycleOwner) {
            updateController(it)
        }

        viewModel.getEditorStreamBundle(args.browseUrl)
        updateController(null)
    }

    private fun updateController(appList: MutableList<App>?) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            if (appList == null) {
                for (i in 1..6) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                appList.forEach { app ->
                    val screenshotsViewModels = mutableListOf<EpoxyModel<*>>()

                    for ((position, artwork) in app.screenshots.withIndex()) {
                        screenshotsViewModels.add(
                            MiniScreenshotViewModel_()
                                .id(artwork.url)
                                .position(position)
                                .artwork(artwork)
                                .callback(object : MiniScreenshotView.ScreenshotCallback {
                                    override fun onClick(position: Int) {
                                        openScreenshotFragment(app, position)
                                    }
                                })
                        )
                    }

                    add(
                        AppListViewModel_()
                            .id("app_${app.id}")
                            .app(app)
                            .click { _ -> openDetailsFragment(app.packageName, app) }
                    )

                    app.editorReason?.let { editorReason ->
                        add(
                            EditorHeadViewModel_()
                                .id("bulletin_${app.id}")
                                .title(
                                    editorReason.bulletins
                                        .joinToString(transform = { "\n• $it" })
                                        .substringAfter(delimiter = "\n")
                                )
                                .click { _ -> openDetailsFragment(app.packageName, app) }
                        )
                    }

                    if (screenshotsViewModels.isNotEmpty()) {
                        add(
                            CarouselHorizontalModel_()
                                .id("screenshots_${app.id}")
                                .models(screenshotsViewModels)
                        )
                    }

                    app.editorReason?.let { editorReason ->
                        if (editorReason.description.isNotEmpty()) {
                            add(
                                EditorHeadViewModel_()
                                    .id("description_${app.id}")
                                    .title(editorReason.description)
                                    .click { _ -> openDetailsFragment(app.packageName, app) }
                            )
                        }
                    }

                    add(
                        HorizontalDividerViewModel_()
                            .id("divider_${app.id}")
                    )
                }
            }
        }
    }
}
