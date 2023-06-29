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
import androidx.lifecycle.ViewModelProvider
import com.airbnb.epoxy.EpoxyModel
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.store.view.epoxy.groups.CarouselHorizontalModel_
import com.aurora.store.view.epoxy.views.EditorHeadViewModel_
import com.aurora.store.view.epoxy.views.HorizontalDividerViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.details.MiniScreenshotView
import com.aurora.store.view.epoxy.views.details.MiniScreenshotViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.viewmodel.editorschoice.EditorBrowseViewModel


class EditorStreamBrowseActivity : BaseActivity() {

    lateinit var B: ActivityGenericRecyclerBinding
    lateinit var VM: EditorBrowseViewModel
    lateinit var title: String

    override fun onConnected() {
        hideNetworkConnectivitySheet()
    }

    override fun onDisconnected() {
        showNetworkConnectivitySheet()
    }

    override fun onReconnected() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        B = ActivityGenericRecyclerBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this)[EditorBrowseViewModel::class.java]

        setContentView(B.root)

        attachToolbar()

        VM.liveData.observe(this) {
            updateController(it)
        }

        intent.apply {
            getStringExtra(Constants.BROWSE_EXTRA)?.let {
                VM.getEditorStreamBundle(it)
            }
            getStringExtra(Constants.STRING_EXTRA)?.let {
                B.layoutToolbarAction.txtTitle.text = it
            }
        }

        updateController(null)
    }

    private fun attachToolbar() {
        B.layoutToolbarAction.imgActionPrimary.setOnClickListener {
            finishAfterTransition()
        }
    }

    private fun updateController(appList: MutableList<App>?) {
        B.recycler.withModels {
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
                                        openScreenshotActivity(app, position)
                                    }
                                })
                        )
                    }

                    add(
                        AppListViewModel_()
                            .id("app_${app.id}")
                            .app(app)
                            .click { _ -> openDetailsActivity(app) }
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
                                .click { _ -> openDetailsActivity(app) }
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
                                    .click { _ -> openDetailsActivity(app) }
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
