/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.aurora.store.R
import com.aurora.store.data.model.AuthState
import com.aurora.store.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.android.awaitFrame

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    // Navigate only once the session is verified. Mirrors SplashScreen.kt, which treats
    // Available/Verifying as transient loading states and advances only on Valid/SignedIn.
    LaunchedEffect(authState) {
        if (authState is AuthState.Valid || authState is AuthState.SignedIn) {
            onLoggedIn()
        }
    }

    // A button is only composed when not in a loading state; focus it once it (re)appears.
    val showsButton = authState !is AuthState.Fetching &&
        authState !is AuthState.Available &&
        authState !is AuthState.Verifying
    LaunchedEffect(showsButton) {
        if (showsButton) {
            // Wait a frame so the focus node is attached before requesting focus.
            awaitFrame()
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.tv_screen_padding)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.app_name))

        when (val state = authState) {
            is AuthState.Fetching,
            is AuthState.Available,
            is AuthState.Verifying -> CircularProgressIndicator()
            is AuthState.Failed -> {
                Text(state.status)
                Button(
                    onClick = { viewModel.buildAnonymousAuthData() },
                    modifier = Modifier.focusRequester(focusRequester)
                ) {
                    Text(stringResource(R.string.action_retry))
                }
            }
            // Init, Unavailable, SignedOut, PendingAccountManager: all "not logged in" on TV,
            // which only offers anonymous login.
            else -> Button(
                onClick = { viewModel.buildAnonymousAuthData() },
                modifier = Modifier.focusRequester(focusRequester)
            ) {
                Text(stringResource(R.string.account_add_anonymous))
            }
        }
    }
}
