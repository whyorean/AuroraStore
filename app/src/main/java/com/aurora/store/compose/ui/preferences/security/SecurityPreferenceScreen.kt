/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.preferences.security

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.util.AppLockAuthenticator
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_APP_LOCK_ENABLED
import com.aurora.store.util.Preferences.PREFERENCE_CONFIRM_EXTERNAL_DEEPLINK
import com.aurora.store.util.save

@Composable
fun SecurityPreferenceScreen() {
    ScreenContent()
}

@Composable
private fun ScreenContent() {
    val context = LocalContext.current
    var appLockEnabled by remember {
        mutableStateOf(Preferences.getBoolean(context, PREFERENCE_APP_LOCK_ENABLED, false))
    }
    var confirmDeepLink by remember {
        mutableStateOf(
            Preferences.getBoolean(context, PREFERENCE_CONFIRM_EXTERNAL_DEEPLINK, true)
        )
    }

    fun setAppLock(enabled: Boolean) {
        // Refuse to enable the lock when the device has no biometric or screen-lock to fall back on
        if (enabled && !AppLockAuthenticator.canAuthenticate(context)) {
            context.toast(R.string.app_lock_no_credential)
            return
        }
        appLockEnabled = enabled
        context.save(PREFERENCE_APP_LOCK_ENABLED, enabled)
    }

    fun setConfirmDeepLink(enabled: Boolean) {
        confirmDeepLink = enabled
        context.save(PREFERENCE_CONFIRM_EXTERNAL_DEEPLINK, enabled)
    }

    Scaffold(
        topBar = { TopAppBar(title = stringResource(R.string.title_security)) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item {
                ListItem(
                    modifier = Modifier.clickable { setAppLock(!appLockEnabled) },
                    headlineContent = { Text(stringResource(R.string.app_lock_title)) },
                    supportingContent = { Text(stringResource(R.string.app_lock_summary)) },
                    trailingContent = {
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { setAppLock(it) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    modifier = Modifier.clickable { setConfirmDeepLink(!confirmDeepLink) },
                    headlineContent = { Text(stringResource(R.string.confirm_deeplink_title)) },
                    supportingContent = { Text(stringResource(R.string.confirm_deeplink_summary)) },
                    trailingContent = {
                        Switch(
                            checked = confirmDeepLink,
                            onCheckedChange = { setConfirmDeepLink(it) }
                        )
                    }
                )
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun SecurityPreferenceScreenPreview() {
    ScreenContent()
}
