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

package com.aurora.store.view.ui.splash

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.aurora.extensions.hide
import com.aurora.extensions.load
import com.aurora.extensions.open
import com.aurora.extensions.show
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.data.AuthState
import com.aurora.store.data.event.BusEvent
import com.aurora.store.databinding.ActivitySplashBinding
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.view.ui.commons.BlacklistActivity
import com.aurora.store.view.ui.spoof.SpoofActivity
import com.aurora.store.viewmodel.auth.AuthViewModel
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class SplashActivity : BaseActivity() {

    private lateinit var VM: AuthViewModel
    private lateinit var B: ActivitySplashBinding

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

        EventBus.getDefault().register(this);

        B = ActivitySplashBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this).get(AuthViewModel::class.java)

        setContentView(B.root)

        B.imgIcon.load(R.drawable.ic_logo) {
            transform(RoundedCorners(32))
        }

        attachToolbar()
        attachActions()

        //Initial status
        updateStatus("Getting things ready..")

        VM.liveData.observe(this, {
            when (it) {
                AuthState.Valid -> {
                    moveToContent()
                }

                AuthState.Available -> {
                    updateStatus("Verifying session")
                    updateActionLayout(false)
                }

                AuthState.Unavailable -> {
                    updateStatus("You need to login first")
                    updateActionLayout(true)
                }

                AuthState.SignedIn -> {
                    moveToContent()
                }

                AuthState.SignedOut -> {
                    updateStatus("Last session scrapped")
                    updateActionLayout(true)
                }

                is AuthState.Status -> {
                    updateStatus(it.status)
                }
            }
        })
    }

    override fun onResume() {
        if (::VM.isInitialized) {
            VM.observe()
        }
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_splash, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_blacklist_manager -> {
                open(BlacklistActivity::class.java)
                return true
            }
            R.id.menu_spoof_manager -> {
                open(SpoofActivity::class.java)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun attachToolbar() {
        setSupportActionBar(B.layoutToolbarAction.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.elevation = 0f
            actionBar.title = ""
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe()
    fun onEventReceived(event: BusEvent) {
        when (event) {
            is BusEvent.GoogleAAS -> {
                if (event.success) {
                    updateStatus("Verifying Google Session")
                    VM.buildGoogleAuthData(event.email, event.aasToken)
                } else {
                    updateStatus("Failed to login via Google")
                }
            }
            else -> {

            }
        }
    }

    private fun updateStatus(string: String?) {
        runOnUiThread {
            B.txtStatus.apply {
                text = string
            }
        }
    }

    private fun updateActionLayout(isVisible: Boolean) {
        if (isVisible) {
            B.layoutAction.show()
        } else {
            B.layoutAction.hide()
        }
    }

    private fun attachActions() {
        B.btnAnonymous.addOnClickListener {
            B.btnAnonymous.updateProgress(true)
            VM.buildAnonymousAuthData()
        }

        B.btnGoogle.addOnClickListener {
            B.btnGoogle.updateProgress(true)
            openGoogleActivity()
        }
    }

    private fun moveToContent() {
        runOnUiThread { open(MainActivity::class.java, true) }
    }
}
