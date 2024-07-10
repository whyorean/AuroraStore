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
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.BusEvent
import com.aurora.store.databinding.FragmentGoogleBinding
import com.aurora.store.util.AC2DMUtil
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GoogleFragment : BaseFragment<FragmentGoogleBinding>() {

    private val viewModel: AuthViewModel by activityViewModels()

    companion object {
        const val EMBEDDED_SETUP_URL = "https://accounts.google.com/EmbeddedSetup"
        const val AUTH_TOKEN = "oauth_token"
        private const val JS_SCRIPT =
            "(function() { return document.getElementById('profileIdentifier').innerHTML; })();"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cookieManager = CookieManager.getInstance()

        binding.webview.apply {
            cookieManager.removeAllCookies(null)
            cookieManager.acceptThirdPartyCookies(this)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)

                    if (newProgress != 0) {
                        binding.progressBar.also {
                            it.isVisible = newProgress < 100
                            it.isIndeterminate = false
                            it.max = 100
                            it.progress = newProgress
                        }
                    } else {
                        binding.progressBar.isIndeterminate = true
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val cookies = CookieManager.getInstance().getCookie(url)
                    // cookies can be null if there is an error
                    if (cookies != null) {
                        val cookieMap = AC2DMUtil.parseCookieString(cookies)
                        if (cookieMap.isNotEmpty() && cookieMap[AUTH_TOKEN] != null) {
                            val oauthToken = cookieMap[AUTH_TOKEN]
                            evaluateJavascript(JS_SCRIPT) {
                                val email = it.replace("\"".toRegex(), "")
                                viewModel.buildAuthData(view.context, email, oauthToken)
                            }
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

        viewLifecycleOwner.lifecycleScope.launch {
            AuroraApp.flowEvent.busEvent.collect { onEventReceived(it) }
        }
    }

    private fun onEventReceived(event: BusEvent) {
        when (event) {
            is BusEvent.GoogleAAS -> {
                if (event.success) {
                    viewModel.buildGoogleAuthData(event.email, event.aasToken)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_aas_token_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }

                findNavController().navigate(
                    GoogleFragmentDirections.actionGoogleFragmentToSplashFragment()
                )
            }

            else -> {}
        }
    }
}
