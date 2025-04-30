/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.app

import androidx.annotation.DrawableRes
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.preview.AppPreviewProvider

/**
 * Composable to show a tag related to an app
 * @param modifier The modifier to be applied to the composable
 * @param label Label of the tag
 * @param icon Icon of the tag
 * @param onClick Callback when this composable is clicked
 */
@Composable
fun AppTagComposable(
    modifier: Modifier = Modifier,
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit = {}
) {
    FilterChip(
        modifier = modifier,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(painter = painterResource(icon), contentDescription = label) },
        selected = true
    )
}

@Preview(showBackground = true)
@Composable
private fun AppTagComposablePreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    AppTagComposable(
        label = stringResource(R.string.details_free),
        icon = R.drawable.ic_paid
    )
}
