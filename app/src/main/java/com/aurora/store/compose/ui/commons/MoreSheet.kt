/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.store.R
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.viewmodel.commons.MoreViewModel

private data class MoreItem(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSheet(
    onDismiss: () -> Unit,
    onNavigateTo: (Destination) -> Unit = {},
    viewModel: MoreViewModel = hiltViewModel()
) {
    val mainItems = listOf(
        MoreItem(R.string.title_apps_games, R.drawable.ic_apps) {
            onNavigateTo(Destination.Installed)
        },
        MoreItem(R.string.title_blacklist_manager, R.drawable.ic_blacklist) {
            onNavigateTo(Destination.Blacklist)
        },
        MoreItem(R.string.title_favourites_manager, R.drawable.ic_favorite_unchecked) {
            onNavigateTo(Destination.Favourite)
        },
        MoreItem(R.string.title_spoof_manager, R.drawable.ic_spoof) {
            onNavigateTo(Destination.Spoof)
        }
    )
    val extraItems = listOf(
        MoreItem(R.string.title_settings, R.drawable.ic_menu_settings) {
            onNavigateTo(Destination.Settings)
        },
        MoreItem(R.string.title_about, R.drawable.ic_menu_about) { onNavigateTo(Destination.About) }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        AccountHeader(
            viewModel = viewModel,
            onNavigateToAccounts = {
                onNavigateTo(Destination.Accounts)
                onDismiss()
            }
        )

        HorizontalDivider()

        Column(
            Modifier.padding(
                horizontal = dimensionResource(R.dimen.padding_small),
                vertical = dimensionResource(R.dimen.padding_small)
            )
        ) {
            mainItems.forEach { item ->
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(item.titleRes)) },
                    selected = false,
                    onClick = {
                        item.onClick()
                        onDismiss()
                    }
                )
            }
        }

        HorizontalDivider()

        Column(
            Modifier.padding(
                horizontal = dimensionResource(R.dimen.padding_small),
                vertical = dimensionResource(R.dimen.padding_small)
            )
        ) {
            extraItems.forEach { item ->
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(item.titleRes)) },
                    selected = false,
                    onClick = {
                        item.onClick()
                        onDismiss()
                    }
                )
            }
        }

        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun AccountHeader(viewModel: MoreViewModel, onNavigateToAccounts: () -> Unit) {
    val context = LocalContext.current
    val isAnonymous = viewModel.authProvider.isAnonymous

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.padding_large)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(viewModel.authProvider.authData?.userProfile?.artwork?.url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            placeholder = painterResource(R.drawable.ic_account),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .requiredSize(40.dp)
                .clip(CircleShape)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimensionResource(R.dimen.padding_normal))
        ) {
            Text(
                text = if (isAnonymous) {
                    stringResource(R.string.account_anonymous)
                } else {
                    viewModel.authProvider.authData?.userProfile?.name
                        ?: stringResource(R.string.status_unavailable)
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = viewModel.authProvider.authData?.userProfile?.email
                    ?: stringResource(R.string.status_unavailable),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onNavigateToAccounts) {
            Icon(
                painter = painterResource(R.drawable.ic_account_manager),
                contentDescription = stringResource(R.string.manage_account)
            )
        }
    }
}
