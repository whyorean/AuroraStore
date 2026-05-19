/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Sheet that lets the user choose how a list of installed apps is sorted and
 * filtered. Edits propagate immediately via [onStateChange]; the sheet itself is
 * stateless.
 *
 * @param installers Map of installer package name to human-readable label, used
 *  to populate the installer filter chips. When empty the installer row is
 *  hidden.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SortFilterSheet(
    state: SortFilterState,
    installers: Map<String, String>,
    onStateChange: (SortFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            SortSection(state = state, onStateChange = onStateChange)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_xsmall))
            )
            FilterSection(
                state = state,
                installers = installers,
                onStateChange = onStateChange
            )
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSection(state: SortFilterState, onStateChange: (SortFilterState) -> Unit) {
    SectionHeader(title = stringResource(R.string.installed_sort_by))
    SortBy.entries.forEach { option ->
        SelectableRow(
            label = stringResource(option.labelRes()),
            selected = state.sortBy == option,
            onClick = { onStateChange(state.copy(sortBy = option)) }
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_xsmall)
            )
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SortOrder.entries.forEachIndexed { index, order ->
                SegmentedButton(
                    selected = state.sortOrder == order,
                    onClick = { onStateChange(state.copy(sortOrder = order)) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = SortOrder.entries.size
                    )
                ) {
                    Text(text = stringResource(order.labelRes()))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    state: SortFilterState,
    installers: Map<String, String>,
    onStateChange: (SortFilterState) -> Unit
) {
    SectionHeader(title = stringResource(R.string.installed_filter))
    AppType.entries.forEach { type ->
        CheckableRow(
            label = stringResource(type.labelRes()),
            checked = type in state.appTypes,
            onCheckedChange = { checked ->
                val next = if (checked) state.appTypes + type else state.appTypes - type
                // Always keep at least one type selected to avoid an empty list.
                if (next.isNotEmpty()) onStateChange(state.copy(appTypes = next))
            }
        )
    }
    if (installers.isNotEmpty()) {
        Text(
            text = stringResource(R.string.installed_filter_installer),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_xsmall)
            )
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacing_medium)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
        ) {
            FilterChip(
                selected = state.installer == null,
                onClick = { onStateChange(state.copy(installer = null)) },
                label = { Text(text = stringResource(R.string.installed_filter_all)) }
            )
            installers.entries.sortedBy { it.value.lowercase() }.forEach { (pkg, label) ->
                FilterChip(
                    selected = state.installer == pkg,
                    onClick = {
                        onStateChange(
                            state.copy(installer = if (state.installer == pkg) null else pkg)
                        )
                    },
                    label = { Text(text = label) }
                )
            }
        }
    }
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_xsmall)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CheckableRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_xsmall)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun SortFilterSheetPreview() {
    SortFilterSheet(
        state = SortFilterState(),
        installers = mapOf(
            "com.android.vending" to "Google Play Store",
            "com.aurora.store" to "Aurora Store",
            "org.fdroid.fdroid" to "F-Droid"
        ),
        onStateChange = {},
        onDismiss = {}
    )
}
