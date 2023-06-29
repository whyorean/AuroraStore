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

package com.aurora.store.view.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.aurora.extensions.browse
import com.aurora.extensions.getEmptyActivityAnimation
import com.aurora.extensions.load
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.store.R
import com.aurora.store.data.AuthState
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.databinding.ActivityAccountBinding
import com.aurora.store.util.Preferences
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.viewmodel.auth.AuthViewModel
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class AccountActivity : BaseActivity() {

    private lateinit var VM: AuthViewModel
    private lateinit var B: ActivityAccountBinding

    private lateinit var authData: AuthData
    private lateinit var accountProvider: AccountProvider

    private val URL_TOS = "https://play.google.com/about/play-terms/"
    private val URL_LICENSE = "https://gitlab.com/AuroraOSS/AuroraStore/blob/master/LICENSE"
    private val URL_DISCLAIMER = "https://gitlab.com/AuroraOSS/AuroraStore/blob/master/DISCLAIMER.md"

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_CANCELED) {
                resetActions()
            } else {
                B.btnGoogle.updateProgress(true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EventBus.getDefault().register(this);

        B = ActivityAccountBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this)[AuthViewModel::class.java]

        setContentView(B.root)

        authData = AuthProvider.with(this).getAuthData()
        accountProvider = AccountProvider.with(this)

        attachToolbar()
        attachChips()
        attachActions()

        updateContents()

        VM.liveData.observe(this) {
            when (it) {
                AuthState.Fetching -> {
                    updateStatus(getString(R.string.requesting_new_session))
                }
                AuthState.Valid -> {
                    updateContents()
                }

                AuthState.InValid -> {
                    updateStatus(getString(R.string.failed_validating_account))
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

    private fun updateContents() {
        if (accountProvider.isSignedIn()) {
            B.viewFlipper.displayedChild = 1
            updateStatus(getString(R.string.session_good))
        } else {
            B.viewFlipper.displayedChild = 0
            updateStatus(getString(R.string.session_enjoy))
        }

        updateUserProfile()
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
            B.layoutAction.visibility = View.VISIBLE
        } else {
            B.layoutAction.visibility = View.INVISIBLE
        }
    }

    private fun attachToolbar() {
        B.layoutToolbarAction.txtTitle.text = getString(R.string.title_account_manager)
        B.layoutToolbarAction.imgActionPrimary.setOnClickListener {
            finishAfterTransition()
        }
    }

    private fun attachChips() {
        B.chipDisclaimer.setOnClickListener { browse(URL_DISCLAIMER) }
        B.chipLicense.setOnClickListener { browse(URL_LICENSE) }
        B.chipTos.setOnClickListener { browse(URL_TOS) }
    }


    private fun attachActions() {
        B.btnAnonymous.updateProgress(false)
        B.btnGoogle.updateProgress(false)

        B.btnAnonymous.addOnClickListener {
            if (VM.liveData.value != AuthState.Fetching) {
                B.btnAnonymous.updateProgress(true)
                VM.buildAnonymousAuthData()
            }
        }

        B.btnGoogle.addOnClickListener {
            if (VM.liveData.value != AuthState.Fetching) {
                B.btnGoogle.updateProgress(true)
                val intent = Intent(this, GoogleActivity::class.java)
                startForResult.launch(intent, getEmptyActivityAnimation())
            }
        }

        B.btnLogout.addOnClickListener {
            task {
                AccountProvider.with(this).logout()
            } successUi {
                B.btnAnonymous.updateProgress(false)
                B.btnGoogle.updateProgress(false)
                updateContents()
            } failUi {

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
    }

    private fun updateUserProfile() {
        authData = AuthProvider.with(this).getAuthData()

        if (accountProvider.isSignedIn()) {
            authData.userProfile?.let {
                B.imgAvatar.load(it.artwork.url) {
                    placeholder(R.drawable.bg_placeholder)
                    transform(RoundedCorners(32))
                }

                B.txtName.text = if (authData.isAnonymous)
                    "Anonymous"
                else
                    it.name

                B.txtEmail.text = if (authData.isAnonymous)
                    "anonymous@gmail.com"
                else
                    it.email
            }
        } else {
            B.imgAvatar.load(R.mipmap.ic_launcher) {
                transform(RoundedCorners(32))
            }
            B.txtName.text = getString(R.string.app_name)
            B.txtEmail.text = getString(R.string.account_logged_out)
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
}
