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

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aurora.Constants
import com.aurora.extensions.close
import com.aurora.extensions.isLAndAbove
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.databinding.ActivityGoogleBinding
import com.aurora.store.util.AC2DMTask
import com.aurora.store.util.AC2DMUtil
import com.aurora.store.util.Preferences
import com.aurora.store.view.ui.commons.BaseActivity
import nl.komponents.kovenant.task
import org.greenrobot.eventbus.EventBus

class GoogleActivity : BaseActivity() {

    private lateinit var B: ActivityGoogleBinding

    private val cookieManager = CookieManager.getInstance()

    override fun onConnected() {
    }

    override fun onDisconnected() {
        showNetworkConnectivitySheet()
    }

    override fun onReconnected() {
        hideNetworkConnectivitySheet()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        B = ActivityGoogleBinding.inflate(layoutInflater)
        setContentView(B.root)

        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        if (isLAndAbove()) {
            cookieManager.removeAllCookies(null)
            cookieManager.acceptThirdPartyCookies(B.webview)
            cookieManager.setAcceptThirdPartyCookies(B.webview, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            B.webview.settings.safeBrowsingEnabled = false
        }

        B.webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val cookies = CookieManager.getInstance().getCookie(url)
                val cookieMap = AC2DMUtil.parseCookieString(cookies)
                if (cookieMap.isNotEmpty() && cookieMap[AUTH_TOKEN] != null) {
                    val oauthToken = cookieMap[AUTH_TOKEN]
                    B.webview.evaluateJavascript("(function() { return document.getElementById('profileIdentifier').innerHTML; })();") {
                        val email = it.replace("\"".toRegex(), "")
                        buildAuthData(email, oauthToken)
                    }
                }
            }
        }

        B.webview.apply {
            settings.apply {
                allowContentAccess = true
                databaseEnabled = true
                domStorageEnabled = true
                javaScriptEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            loadUrl(EMBEDDED_SETUP_URL)
        }
    }

    private fun buildAuthData(email: String, oauthToken: String?) {
        task {
            AC2DMTask().getAC2DMResponse(email, oauthToken)
        } success {
            if (it.isNotEmpty()) {
                val aasToken = it["Token"]
                if (aasToken != null) {
                    Preferences.putString(this, Constants.ACCOUNT_EMAIL_PLAIN, email)
                    Preferences.putString(this, Constants.ACCOUNT_AAS_PLAIN, aasToken)
                    EventBus.getDefault().post(BusEvent.GoogleAAS(true, email, aasToken))
                } else {
                    Preferences.putString(this, Constants.ACCOUNT_EMAIL_PLAIN, "")
                    Preferences.putString(this, Constants.ACCOUNT_AAS_PLAIN, "")
                    EventBus.getDefault().post(BusEvent.GoogleAAS(false))
                }
            } else {
                Toast.makeText(this, getString(R.string.toast_aas_token_failed), Toast.LENGTH_LONG)
                    .show()
                EventBus.getDefault().post(BusEvent.GoogleAAS(false))
            }

            //Close Activity
            setResult(RESULT_OK)
            close()
        } fail {
            Toast.makeText(this, getString(R.string.toast_aas_token_failed), Toast.LENGTH_LONG)
                .show()
            EventBus.getDefault().post(BusEvent.GoogleAAS(false))

            //Close Activity
            setResult(RESULT_CANCELED)
            close()
        }
    }

    companion object {
        const val EMBEDDED_SETUP_URL =
            "https://accounts.google.com/EmbeddedSetup/identifier?flowName=EmbeddedSetupAndroid"
        const val AUTH_TOKEN = "oauth_token"
    }
}
