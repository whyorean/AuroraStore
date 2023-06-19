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

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.aurora.extensions.getEmptyActivityAnimation
import com.aurora.extensions.hide
import com.aurora.extensions.load
import com.aurora.extensions.open
import com.aurora.extensions.show
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.data.AuthState
import com.aurora.store.data.event.BusEvent
import com.aurora.store.databinding.ActivitySplashBinding
import com.aurora.store.util.Preferences
import com.aurora.store.view.ui.account.AccountActivity
import com.aurora.store.view.ui.account.GoogleActivity
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.view.ui.commons.BlacklistActivity
import com.aurora.store.view.ui.preferences.SettingsActivity
import com.aurora.store.view.ui.spoof.SpoofActivity
import com.aurora.store.viewmodel.auth.AuthViewModel
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class SplashActivity : BaseActivity() {

    private lateinit var VM: AuthViewModel
    private lateinit var B: ActivitySplashBinding

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_CANCELED) {
                resetActions()
            } else {
                B.btnGoogle.updateProgress(true)
            }
        }

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
        VM = ViewModelProvider(this)[AuthViewModel::class.java]

        setContentView(B.root)

        B.imgIcon.load(R.drawable.ic_logo) {
            transform(RoundedCorners(32))
        }

        attachToolbar()
        attachActions()

        //Initial status
        updateStatus(getString(R.string.session_init))

        VM.liveData.observe(this) {
            when (it) {
                AuthState.Fetching -> {
                    updateStatus(getString(R.string.requesting_new_session))
                }
                AuthState.Valid -> {
                    moveToContent()
                }

                AuthState.InValid -> {
                    updateStatus(getString(R.string.failed_validating_account))
                    updateActionLayout(true)
                }

                AuthState.Available -> {
                    updateStatus(getString(R.string.session_verifying))
                    updateActionLayout(false)
                }

                AuthState.Unavailable -> {
                    updateStatus(getString(R.string.session_login))
                    updateActionLayout(true)
                }

                AuthState.SignedIn -> {
                    VM.observe()
                }

                AuthState.SignedOut -> {
                    updateStatus(getString(R.string.session_scrapped))
                    updateActionLayout(true)
                }

                is AuthState.Status -> {
                    updateStatus(it.status)
                    resetActions()
                }
            }
        }
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
            R.id.menu_account_manager -> {
                open(AccountActivity::class.java)
                return true
            }
            R.id.menu_settings -> {
                open(SettingsActivity::class.java)
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
                    updateStatus(getString(R.string.session_verifying_google))
                    VM.buildGoogleAuthData(event.email, event.aasToken)
                } else {
                    updateStatus(getString(R.string.session_login_failed_google))
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
            if (VM.liveData.value != AuthState.Fetching) {
                B.btnAnonymous.updateProgress(true)
                VM.buildAnonymousAuthData()
            }
        }

        B.btnAnonymousInsecure.addOnClickListener {
            if (VM.liveData.value != AuthState.Fetching) {
                B.btnAnonymousInsecure.updateProgress(true)
                VM.buildInSecureAnonymousAuthData()
            }
        }

        B.btnGoogle.addOnClickListener {
            if (VM.liveData.value != AuthState.Fetching) {
                B.btnGoogle.updateProgress(true)
                val intent = Intent(this, GoogleActivity::class.java)
                startForResult.launch(intent, getEmptyActivityAnimation())
            }
        }
    }

    private fun resetActions() {
        B.btnGoogle.apply {
            updateProgress(false)
            isEnabled = true
        }

        B.btnAnonymous.apply {
            updateProgress(false)
            isEnabled = true
        }

        B.btnAnonymousInsecure.apply {
            updateProgress(false)
            isEnabled = true
        }
    }

    private fun moveToContent() {
        runOnUiThread { open(MainActivity::class.java, true) }
    }
}
