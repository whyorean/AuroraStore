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

import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aurora.Constants
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.databinding.FragmentGoogleBinding
import com.aurora.store.util.AC2DMTask
import com.aurora.store.util.AC2DMUtil
import com.aurora.store.util.Preferences
import com.aurora.store.viewmodel.auth.AuthViewModel
import nl.komponents.kovenant.task
import org.greenrobot.eventbus.EventBus

class GoogleFragment : Fragment(R.layout.fragment_google) {

    private val viewModel: AuthViewModel by activityViewModels()

    companion object {
        const val EMBEDDED_SETUP_URL =
            "https://accounts.google.com/EmbeddedSetup/identifier?flowName=EmbeddedSetupAndroid"
        const val AUTH_TOKEN = "oauth_token"
        private const val JS_SCRIPT =
            "(function() { return document.getElementById('profileIdentifier').innerHTML; })();"
    }

    private var _binding: FragmentGoogleBinding? = null
    private val binding: FragmentGoogleBinding
        get() = _binding!!

    private val cookieManager = CookieManager.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGoogleBinding.bind(view)

        binding.webview.apply {
            cookieManager.removeAllCookies(null)
            cookieManager.acceptThirdPartyCookies(this)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val cookies = CookieManager.getInstance().getCookie(url)
                    val cookieMap = AC2DMUtil.parseCookieString(cookies)
                    if (cookieMap.isNotEmpty() && cookieMap[AUTH_TOKEN] != null) {
                        val oauthToken = cookieMap[AUTH_TOKEN]
                        evaluateJavascript(JS_SCRIPT) {
                            val email = it.replace("\"".toRegex(), "")
                            buildAuthData(email, oauthToken)
                        }
                    }
                }
            }

            settings.apply {
                allowContentAccess = true
                databaseEnabled = true
                domStorageEnabled = true
                javaScriptEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = false
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
                    Preferences.putString(requireContext(), Constants.ACCOUNT_EMAIL_PLAIN, email)
                    Preferences.putString(requireContext(), Constants.ACCOUNT_AAS_PLAIN, aasToken)
                    EventBus.getDefault().post(BusEvent.GoogleAAS(true, email, aasToken))
                    viewModel.buildGoogleAuthData(email, aasToken)
                } else {
                    Preferences.putString(requireContext(), Constants.ACCOUNT_EMAIL_PLAIN, "")
                    Preferences.putString(requireContext(), Constants.ACCOUNT_AAS_PLAIN, "")
                    EventBus.getDefault().post(BusEvent.GoogleAAS(false))
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_aas_token_failed),
                    Toast.LENGTH_LONG
                ).show()
                EventBus.getDefault().post(BusEvent.GoogleAAS(false))
            }
            findNavController().navigate(
                GoogleFragmentDirections.actionGoogleFragmentToSplashFragment()
            )
        } fail {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_aas_token_failed),
                Toast.LENGTH_LONG
            ).show()
            EventBus.getDefault().post(BusEvent.GoogleAAS(false))
            findNavController().navigate(
                GoogleFragmentDirections.actionGoogleFragmentToSplashFragment()
            )
        }
    }
}
