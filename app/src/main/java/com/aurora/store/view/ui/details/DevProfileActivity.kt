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

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.data.models.details.DevStream
import com.aurora.store.R
import com.aurora.store.data.ViewState
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.ActivityDevProfileBinding
import com.aurora.extensions.close
import com.aurora.extensions.load
import com.aurora.store.view.epoxy.controller.DeveloperCarouselController
import com.aurora.store.view.epoxy.controller.GenericCarouselController
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.viewmodel.details.DevProfileViewModel

class DevProfileActivity : BaseActivity(), GenericCarouselController.Callbacks {

    private lateinit var B: ActivityDevProfileBinding
    private lateinit var VM: DevProfileViewModel
    private lateinit var C: DeveloperCarouselController
    private lateinit var authData: AuthData

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
        B = ActivityDevProfileBinding.inflate(layoutInflater)
        C = DeveloperCarouselController(this)
        VM = ViewModelProvider(this)[DevProfileViewModel::class.java]

        authData = AuthProvider.with(this).getAuthData()

        setContentView(B.root)

        attachToolbar()
        attachRecycler()

        VM.liveData.observe(this, {
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
                    updateInfo(it.data as DevStream)
                    updateController(it.data)
                }
            }
        })

        B.viewFlipper.displayedChild = 1

        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            if (intent.scheme != null && (intent.scheme == "http" || intent.scheme == "https")) {
                val devId = intent.data!!.getQueryParameter("id")
                if (devId.isNullOrEmpty()) {
                    close()
                } else {
                    VM.getStreamBundle(devId)
                }
            } else {
                intent.getStringExtra(Constants.BROWSE_EXTRA)?.let {
                    VM.getStreamBundle(it.substringAfter("developer-"))
                }
                intent.getStringExtra(Constants.STRING_EXTRA)?.let {
                    B.layoutToolbarAction.txtTitle.text = it
                }
            }
        } else {
            close()
        }
    }

    private fun attachToolbar() {
        B.layoutToolbarAction.toolbar.setOnClickListener {
            close()
        }
        B.layoutToolbarAction.txtTitle.text = getString(R.string.details_dev_profile)
    }

    private fun attachRecycler() {
        B.recycler.setController(C)
    }

    private fun updateInfo(devStream: DevStream) {
        B.layoutToolbarAction.txtTitle.text = devStream.title
        B.txtDevName.text = devStream.title
        B.txtDevDescription.text = devStream.description
        B.imgIcon.load(devStream.imgUrl)
        B.viewFlipper.displayedChild = 0
    }

    private fun updateController(devStream: DevStream) {
        C.setData(devStream.streamBundle)
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {
        openStreamBrowseActivity(streamCluster.clusterBrowseUrl, streamCluster.clusterTitle)
    }

    override fun onClusterScrolled(streamCluster: StreamCluster) {
        VM.observeCluster(streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsActivity(app)
    }

    override fun onAppLongClick(app: App) {

    }
}
