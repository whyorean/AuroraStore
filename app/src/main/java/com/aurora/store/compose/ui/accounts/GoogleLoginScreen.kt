/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.accounts

import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.event.AuthEvent
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.util.AC2DMUtil
import com.aurora.store.viewmodel.auth.AuthViewModel

private const val EMBEDDED_SETUP_URL = "https://accounts.google.com/EmbeddedSetup"
private const val AUTH_TOKEN = "oauth_token"
private const val JS_PROFILE_EMAIL =
    "(function() { return document.getElementById('profileIdentifier').innerHTML; })();"

@Composable
fun GoogleLoginScreen(
    onNavigateTo: (Destination) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var isIndeterminate by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        AuroraApp.events.authEvent.collect { event ->
            if (event is AuthEvent.GoogleLogin) {
                if (event.success) {
                    AccountProvider.login(
                        context,
                        event.email,
                        event.token,
                        AuthHelper.Token.AAS,
                        AccountType.GOOGLE
                    )
                } else {
                    Toast.makeText(context, R.string.toast_aas_token_failed, Toast.LENGTH_LONG)
                        .show()
                }
                onNavigateTo(Destination.Splash)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            if (isIndeterminate) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val cookieManager = CookieManager.getInstance()
                WebView(ctx).apply {
                    cookieManager.removeAllCookies(null)
                    cookieManager.acceptThirdPartyCookies(this)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            if (newProgress != 0) {
                                isIndeterminate = false
                                progress = newProgress / 100f
                                isLoading = newProgress < 100
                            } else {
                                isIndeterminate = true
                                isLoading = true
                            }
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            val cookies = CookieManager.getInstance().getCookie(url)
                            if (cookies != null) {
                                val cookieMap = AC2DMUtil.parseCookieString(cookies)
                                if (cookieMap.isNotEmpty() && cookieMap[AUTH_TOKEN] != null) {
                                    val oauthToken = cookieMap[AUTH_TOKEN]
                                    view.evaluateJavascript(JS_PROFILE_EMAIL) { result ->
                                        val email = result.replace("\"", "")
                                        viewModel.buildAuthData(view.context, email, oauthToken)
                                    }
                                }
                            }
                        }
                    }

                    settings.apply {
                        allowContentAccess = true
                        domStorageEnabled = true
                        javaScriptEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            safeBrowsingEnabled = false
                        }
                    }

                    loadUrl(EMBEDDED_SETUP_URL)
                }
            }
        )
    }
}
