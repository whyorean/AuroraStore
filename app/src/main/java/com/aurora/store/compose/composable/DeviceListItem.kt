/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * Composable to display device details for spoofing in a list
 * @param modifier The modifier to be applied to the composable
 * @param userReadableName Name of the device, obtained through `UserReadableName` property
 * @param manufacturer Name of the device manufacturer, obtained through `Build.MANUFACTURER` property
 * @param androidVersionSdk Android version on the device, obtained through `Build.VERSION.SDK_INT` property
 * @param platforms Platforms supported on the device, obtained through `Platforms` property
 * @param isChecked If the device is selected
 * @param onClick Callback when the composable is clicked
 */
@Composable
fun DeviceListItem(
    modifier: Modifier = Modifier,
    userReadableName: String,
    manufacturer: String,
    androidVersionSdk: String,
    platforms: String,
    isChecked: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { if (!isChecked) onClick() })
            .padding(dimensionResource(R.dimen.padding_small)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = userReadableName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.spoof_property, manufacturer, androidVersionSdk),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = platforms.replace(",\\s*".toRegex(), ", "),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Checkbox(checked = isChecked, onCheckedChange = { if (!isChecked) onClick() })
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceListItemPreview() {
    DeviceListItem(
        userReadableName = "Google Pixel 7a",
        manufacturer = "Google",
        androidVersionSdk = "33",
        platforms = "arm64-v8a",
        isChecked = true
    )
}
