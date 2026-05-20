/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

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
    AuroraListItem(
        modifier = modifier,
        headline = userReadableName,
        supporting = stringResource(R.string.spoof_property, manufacturer, androidVersionSdk),
        tertiary = platforms.replace(",\\s*".toRegex(), ", "),
        onClick = { if (!isChecked) onClick() },
        trailing = {
            Checkbox(checked = isChecked, onCheckedChange = { if (!isChecked) onClick() })
        }
    )
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
