/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.gplayapi.data.models.App
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.util.CommonUtil.addSiPrefix

/**
 * Composable for displaying minimal app details in a horizontal-scrollable list
 * @param app [App] to display
 * @param onClick Callback when the composable is clicked
 * @see AppListComposable
 */
@Composable
fun AppComposable(app: App, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .width(dimensionResource(R.dimen.icon_size_cluster))
            .clickable { onClick() }
            .padding(dimensionResource(R.dimen.padding_xsmall))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(app.iconArtwork.url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            placeholder = painterResource(R.drawable.ic_android),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .requiredSize(dimensionResource(R.dimen.icon_size_cluster))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
        )
        Text(
            text = app.displayName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (app.size > 0) addSiPrefix(app.size) else app.downloadString,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppComposablePreview() {
    AppComposable(
        app = App(
            packageName = BuildConfig.APPLICATION_ID,
            displayName = LocalContext.current.getString(R.string.app_name),
            size = 7431013
        )
    )
}
