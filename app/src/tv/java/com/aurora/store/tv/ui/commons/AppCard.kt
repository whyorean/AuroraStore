/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.ui.commons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R

/**
 * Banner-style focusable app card: the app's [App.coverArtwork] banner fills the card with a
 * bottom scrim, and the icon + name + developer are overlaid in the lower-left for legibility.
 * Falls back to a solid surface background when the app has no banner.
 */
@Composable
fun AppCard(app: App, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hasBanner = app.coverArtwork.url.isNotBlank()
    var focused by remember { mutableStateOf(false) }

    // Apply the default width first so a caller-supplied modifier can still override it. Raise the
    // focused card above its neighbours so its scaled-up state draws over them instead of under.
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(dimensionResource(R.dimen.tv_app_card_width))
            .zIndex(if (focused) 1f else 0f)
            .onFocusChanged { focused = it.hasFocus }
            .then(modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(
                    if (hasBanner) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            if (hasBanner) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(app.coverArtwork.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = app.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }

            // Scrim so the overlaid icon/name/developer stay readable over any banner.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_small)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_small)
                )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(app.iconArtwork.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.tv_app_card_icon))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.displayName,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = app.developerName,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
