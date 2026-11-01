/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.LayoutDirection
import com.aurora.store.R
import com.aurora.store.util.PackageUtil

@Composable
fun VersionChangeText(
    packageName: String,
    versionName: String,
    versionCode: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = Color.Unspecified
) {
    val context = LocalContext.current
    val updateVersion = stringResource(R.string.version, versionName, versionCode)
    val localVersion = stringResource(
        R.string.version,
        PackageUtil.getInstalledVersionName(context, packageName),
        PackageUtil.getInstalledVersionCode(context, packageName)
    )
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Text(
        modifier = modifier,
        style = style,
        color = color,
        text = buildAnnotatedString {
            append(if (isRtl) updateVersion else localVersion)
            append(" ")
            appendInlineContent("iconId", "[arrow]")
            append(" ")
            append(if (isRtl) localVersion else updateVersion)
        },
        inlineContent = mapOf(
            "iconId" to InlineTextContent(
                Placeholder(
                    width = style.fontSize,
                    height = style.fontSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null
                )
            }
        )
    )
}
