/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import android.text.format.DateUtils
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.room.favourite.Favourite

@Composable
fun FavouriteListItem(
    modifier: Modifier = Modifier,
    favourite: Favourite,
    onClick: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    val context = LocalContext.current
    RemovableListItem(onRemove = onClear) { triggerRemove ->
        AuroraListItem(
            modifier = modifier,
            headline = favourite.displayName,
            supporting = favourite.packageName,
            tertiary = DateUtils.formatDateTime(
                context,
                favourite.added,
                DateUtils.FORMAT_SHOW_DATE
            ),
            headlineStyle = MaterialTheme.typography.bodyMedium,
            onClick = onClick,
            leading = {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(favourite.iconURL)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
                )
            },
            trailing = {
                IconButton(onClick = triggerRemove) {
                    Icon(
                        painter = painterResource(R.drawable.ic_favorite_checked),
                        contentDescription = stringResource(R.string.action_favourite)
                    )
                }
            }
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun FavouriteListItemPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    FavouriteListItem(favourite = Favourite.fromApp(app, Favourite.Mode.MANUAL))
}
