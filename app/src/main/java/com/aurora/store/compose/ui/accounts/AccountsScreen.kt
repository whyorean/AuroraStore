/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.Constants.URL_DISCLAIMER
import com.aurora.Constants.URL_LICENSE
import com.aurora.Constants.URL_TOS
import com.aurora.extensions.browse
import com.aurora.store.R
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.viewmodel.accounts.AccountsViewModel

@Composable
fun AccountsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToSplash: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val userProfile = viewModel.authProvider.authData?.userProfile
    val avatar = when {
        viewModel.authProvider.isAnonymous -> R.mipmap.ic_launcher
        userProfile != null -> userProfile.artwork.url
        else -> R.mipmap.ic_launcher
    }
    val name = when {
        viewModel.authProvider.isAnonymous -> stringResource(R.string.account_anonymous)
        userProfile != null -> userProfile.name
        else -> stringResource(R.string.account_anonymous)
    }
    val email = when {
        viewModel.authProvider.isAnonymous -> stringResource(R.string.account_anonymous_email)
        userProfile != null -> userProfile.email
        else -> stringResource(R.string.account_anonymous_email)
    }

    val context = LocalContext.current
    var shouldShowLogoutDialog by rememberSaveable { mutableStateOf(false) }

    if (shouldShowLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                shouldShowLogoutDialog = false
                AccountProvider.logout(context)
                onNavigateToSplash()
            },
            onDismiss = { shouldShowLogoutDialog = false }
        )
    }

    ScreenContent(
        onNavigateUp = onNavigateUp,
        avatar = avatar,
        name = name,
        email = email,
        onLogout = { shouldShowLogoutDialog = true }
    )
}

@Composable
private fun ScreenContent(
    avatar: Any = R.mipmap.ic_launcher,
    name: String = stringResource(R.string.account_anonymous),
    email: String = stringResource(R.string.account_anonymous_email),
    onNavigateUp: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBarComposable(
                title = stringResource(R.string.title_account_manager),
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(vertical = dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AssistHeader()

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .crossfade(true)
                        .data(avatar)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .requiredSize(dimensionResource(R.dimen.icon_size_avatar))
                        .clip(CircleShape)
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(R.dimen.height_bottom_adj)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
            ) {
                Text(
                    text = stringResource(R.string.account_logout),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                OutlinedButton(onClick = onLogout) {
                    Icon(
                        painter = painterResource(R.drawable.ic_logout),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_xsmall)))
                    Text(
                        text = stringResource(R.string.action_logout)
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistHeader() {
    val context = LocalContext.current
    val links = mapOf(
        R.string.menu_terms to URL_TOS,
        R.string.menu_disclaimer to URL_DISCLAIMER,
        R.string.menu_license to URL_LICENSE
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_normal))
    ) {
        items(items = links.keys.toList(), key = { item -> item }) { label ->
            AssistChip(
                onClick = { context.browse(links.getValue(label)) },
                label = { Text(text = stringResource(label)) }
            )
        }
    }
}

@Preview
@Composable
private fun AccountsScreenPreview() {
    ScreenContent()
}
