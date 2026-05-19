/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider

@Composable
fun BlackListItem(
    modifier: Modifier = Modifier,
    icon: Bitmap,
    displayName: String,
    packageName: String,
    versionName: String,
    versionCode: Long,
    isChecked: Boolean = false,
    isEnabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    AuroraListItem(
        modifier = modifier,
        headline = displayName,
        supporting = packageName,
        tertiary = stringResource(R.string.version, versionName, versionCode),
        headlineStyle = MaterialTheme.typography.bodyMedium,
        onClick = onClick,
        enabled = isEnabled,
        leading = {
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
                colorFilter = if (isChecked) DESATURATE else null
            )
        },
        trailing = {
            Checkbox(checked = isChecked, enabled = isEnabled, onCheckedChange = { onClick() })
        }
    )
}

private val DESATURATE = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun BlackListItemPreview() {
    BlackListItem(
        icon = Color.GRAY.toDrawable().toBitmap(56, 56),
        displayName = stringResource(R.string.app_name),
        packageName = BuildConfig.APPLICATION_ID,
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE.toLong(),
        isChecked = true,
        isEnabled = false
    )
}
