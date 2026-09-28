/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.store.R
import com.aurora.store.data.room.account.Account

@Composable
fun AccountListItem(account: Account, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AuroraListItem(
        modifier = modifier,
        headline = if (account.isAnonymous) {
            stringResource(R.string.account_anonymous)
        } else {
            account.displayName ?: account.email
        },
        supporting = account.email,
        onClick = onClick,
        leading = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .crossfade(true)
                    .data(account.profilePicUrl ?: R.mipmap.ic_launcher)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .requiredSize(dimensionResource(R.dimen.icon_size_small))
                    .clip(CircleShape)
            )
        },
        trailing = if (account.isDefault) {
            {
                Text(
                    text = stringResource(R.string.account_default),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
                        )
                        .padding(
                            horizontal = dimensionResource(R.dimen.spacing_small),
                            vertical = dimensionResource(R.dimen.spacing_xsmall)
                        )
                )
            }
        } else {
            null
        },
        headlineStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge
    )
}
