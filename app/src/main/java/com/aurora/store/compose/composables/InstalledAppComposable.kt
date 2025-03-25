/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.aurora.extensions.bodyVerySmall
import com.aurora.store.BuildConfig
import com.aurora.store.R

/**
 * Composable for displaying installed package details in a list
 * @param icon Icon for the package
 * @param displayName User-readable name of the package
 * @param packageName Name of the package
 * @param versionName versionName of the package
 * @param versionCode versionCode of the package
 * @param onClick Callback when the composable is clicked
 * @param onLongClick Callback whe composable is long clicked
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun InstalledAppComposable(
    icon: Bitmap,
    displayName: String,
    packageName: String,
    versionName: String,
    versionCode: Long,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(
                horizontal = dimensionResource(R.dimen.padding_medium),
                vertical = dimensionResource(R.dimen.padding_xsmall)
            )
    ) {
        Row {
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium))
            )
            Column(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.margin_small)),
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.version, versionName, versionCode),
                    style = MaterialTheme.typography.bodyVerySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InstalledAppComposablePreview() {
    InstalledAppComposable(
        icon = Color.GRAY.toDrawable().toBitmap(56, 56),
        displayName = LocalContext.current.getString(R.string.app_name),
        packageName = BuildConfig.APPLICATION_ID,
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE.toLong()
    )
}
