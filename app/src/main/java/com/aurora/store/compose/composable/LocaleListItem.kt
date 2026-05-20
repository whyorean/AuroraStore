/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import java.util.Locale

@Composable
fun LocaleListItem(
    modifier: Modifier = Modifier,
    displayName: String,
    displayLanguage: String,
    isChecked: Boolean = false,
    onClick: () -> Unit = {}
) {
    AuroraListItem(
        modifier = modifier,
        headline = displayName,
        supporting = displayLanguage,
        onClick = { if (!isChecked) onClick() },
        trailing = {
            Checkbox(checked = isChecked, onCheckedChange = { if (!isChecked) onClick() })
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun LocaleListItemPreview() {
    LocaleListItem(
        displayName = Locale.JAPANESE.displayName,
        displayLanguage = Locale.JAPAN.getDisplayLanguage(Locale.JAPAN),
        isChecked = true
    )
}
