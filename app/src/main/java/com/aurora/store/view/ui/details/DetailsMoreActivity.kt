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
import androidx.core.text.HtmlCompat
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.R
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.ActivityDetailsMoreBinding
import com.aurora.store.util.extensions.close
import com.aurora.store.view.epoxy.views.FileViewModel_
import com.aurora.store.view.epoxy.views.HeaderViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppAltViewModel_
import com.aurora.store.view.epoxy.views.details.AppDependentViewModel_
import com.aurora.store.view.epoxy.views.details.MoreBadgeViewModel_
import com.aurora.store.view.ui.commons.BaseActivity
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

class DetailsMoreActivity : BaseActivity() {

    private lateinit var B: ActivityDetailsMoreBinding
    private lateinit var app: App

    override fun onConnected() {

    }

    override fun onDisconnected() {

    }

    override fun onReconnected() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        B = ActivityDetailsMoreBinding.inflate(layoutInflater)
        setContentView(B.root)

        attachToolbar()

        val itemRaw: String? = intent.getStringExtra(Constants.STRING_EXTRA)
        if (itemRaw != null) {
            app = gson.fromJson(itemRaw, App::class.java)
            app.let {
                inflateDescription(app)
                inflateFiles(app)
                fetchDependentApps(app)
            }
        }
    }

    private fun attachToolbar() {
        B.layoutToolbarActionMore.toolbar.setOnClickListener {
            close()
        }
    }

    private fun inflateDescription(app: App) {
        B.layoutToolbarActionMore.txtTitle.text = app.displayName
        B.txtDescription.text = HtmlCompat.fromHtml(
            app.description,
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    }

    private fun inflateFiles(app: App) {
        B.recyclerMore.withModels {
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
        }
    }

    private fun fetchDependentApps(app: App) {
        val authData: AuthData = AuthProvider
            .with(this)
            .getAuthData()
        task {
            AppDetailsHelper(authData)
                .getAppByPackageName(app.dependencies.dependentPackages)
        } successUi {
            B.recyclerDependency.withModels {
                if (it.isNotEmpty()) {
                    it.filter { it.displayName.isNotEmpty() }.forEach {
                        add(
                            AppDependentViewModel_()
                                .id(it.id)
                                .app(it)
                                .click { _ -> openDetailsActivity(it) }
                        )
                    }
                } else {
                    add(
                        NoAppAltViewModel_()
                            .id("no_app")
                            .message(getString(R.string.details_no_dependencies))
                    )
                }
            }
        } failUi {
            B.recyclerDependency.withModels {
                add(
                    NoAppAltViewModel_()
                        .id("no_app")
                        .message(getString(R.string.details_no_dependencies))
                )
            }
        }
    }
}
