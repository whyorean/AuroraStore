/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.aurora.store.compose.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurora.store.R
import com.aurora.store.data.model.VersionEntry
import com.aurora.store.data.model.formatExodusDate

/**
 * Bottom sheet that lists the versions known to Exodus for an app. Selecting one hands its version
 * code back to the manual download screen; the list scrolls internally so long histories stay
 * manageable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionPickerSheet(
    versions: List<VersionEntry>,
    loading: Boolean,
    onSelect: (VersionEntry) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.manual_download_select_version),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.spacing_medium),
                    vertical = dimensionResource(R.dimen.spacing_xsmall)
                )
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))

            AvailabilityWarning(
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.spacing_small)
                )
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))

            when {
                loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.spacing_large)),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }

                versions.isEmpty() -> Text(
                    text = stringResource(R.string.manual_download_no_versions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(R.dimen.spacing_medium),
                        vertical = dimensionResource(R.dimen.spacing_medium)
                    )
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .padding(horizontal = dimensionResource(R.dimen.spacing_small))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    // Positional keys: Exodus can hold more than one report per version code
                    itemsIndexed(items = versions) { index, entry ->
                        if (index > 0) HorizontalDivider()
                        VersionRow(entry = entry, onClick = { onSelect(entry) })
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun AvailabilityWarning(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.height(dimensionResource(R.dimen.icon_size_default)),
                painter = painterResource(R.drawable.ic_help),
                contentDescription = null
            )
            Text(
                text = stringResource(R.string.manual_download_availability_warning),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun VersionRow(entry: VersionEntry, onClick: () -> Unit) {
    val title = if (entry.version.isBlank()) {
        entry.versionCode.toString()
    } else {
        "${entry.version} (${entry.versionCode})"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (entry.date.isNotBlank()) {
            Text(
                text = formatExodusDate(entry.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
