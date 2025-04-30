/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R
import java.util.Locale

/**
 * Composable to display locale details in a list
 * @param modifier The modifier to be applied to the composable
 * @param displayName Display name of the locale
 * @param displayLanguage Display name of the language in the locale
 * @param isChecked Whether the locale is checked/selected
 * @param onClick Callback when the composable is clicked
 */
@Composable
fun LocaleComposable(
    modifier: Modifier = Modifier,
    displayName: String,
    displayLanguage: String,
    isChecked: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(dimensionResource(R.dimen.padding_small)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = displayLanguage,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Checkbox(checked = isChecked, enabled = !isChecked, onCheckedChange = { onClick() })
    }
}

@Preview(showBackground = true)
@Composable
private fun LocaleComposablePreview() {
    LocaleComposable(
        displayName = Locale.JAPANESE.displayName,
        displayLanguage = Locale.JAPAN.getDisplayLanguage(Locale.JAPAN),
        isChecked = true
    )
}
