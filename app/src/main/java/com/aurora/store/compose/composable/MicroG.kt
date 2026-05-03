/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.util.fastForEach
import com.aurora.extensions.browse
import com.aurora.store.R
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.Link
import com.aurora.store.util.CommonUtil.getETAString
import com.aurora.store.viewmodel.onboarding.MicroGItem
import com.aurora.store.viewmodel.onboarding.MicroGItemState
import com.aurora.store.viewmodel.onboarding.MicroGUIState

/**
 * Composable to display suggestion to install microG
 * @param modifier Modifier for the composable
 * @param onInstall Callback when user requests installing microG bundle
 * @param onRetry Callback when user retries after a failed download
 * @param onTOSChecked Callback when user toggles the TOS checkbox
 */
@Composable
fun MicroG(
    modifier: Modifier = Modifier,
    uiState: MicroGUIState,
    onInstall: () -> Unit = {},
    onRetry: () -> Unit = {},
    onTOSChecked: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var isChecked by rememberSaveable { mutableStateOf(false) }
    val links = listOf(
        Link(
            id = 2,
            title = stringResource(R.string.details_dev_website),
            subtitle = stringResource(R.string.microg_website),
            icon = R.drawable.ic_network,
            url = "https://microG.org"
        ),
        Link(
            id = 4,
            title = stringResource(R.string.privacy_policy_title),
            subtitle = stringResource(R.string.microg_privacy_policy),
            icon = R.drawable.ic_privacy,
            url = "https://microg.org/privacy.html"
        ),
        Link(
            id = 5,
            title = stringResource(R.string.menu_disclaimer),
            subtitle = stringResource(R.string.microg_license_agreement),
            icon = R.drawable.ic_disclaimer,
            url = "https://raw.githubusercontent.com/microg/GmsCore/refs/heads/master/LICENSE"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = dimensionResource(R.dimen.padding_small),
                vertical = dimensionResource(R.dimen.padding_xxsmall)
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xxsmall))
        ) {
            Text(
                text = stringResource(R.string.onboarding_gms_microg),
                style = MaterialTheme.typography.bodyMedium
            )

            links.fastForEach { link ->
                LinkListItem(
                    link = link,
                    onClick = { context.browse(link.url) },
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen.padding_large)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            uiState.items.fastForEach { item ->
                MicroGItemRow(item = item)
            }

            if (!uiState.isInProgress && !uiState.isInstalled && !uiState.hasFailed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.padding_small)
                    )
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = {
                            isChecked = it
                            onTOSChecked(it)
                        }
                    )
                    Text(
                        text = stringResource(R.string.onboarding_gms_agreement),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!uiState.isInProgress && !uiState.isInstalled && !uiState.isOnline) {
                Text(
                    text = stringResource(R.string.microg_no_network),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!uiState.isInProgress && !uiState.isInstalled) {
                val isRetry = uiState.hasFailed
                Button(
                    onClick = if (isRetry) onRetry else onInstall,
                    enabled = uiState.isOnline && (isRetry || isChecked),
                    colors = if (isRetry) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isRetry) {
                            stringResource(R.string.action_retry)
                        } else {
                            stringResource(R.string.action_install_microG)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MicroGItemRow(item: MicroGItem) {
    val context = LocalContext.current
    val inProgress = item.state == MicroGItemState.DOWNLOADING ||
        item.state == MicroGItemState.INSTALLING

    val statusText = when (item.state) {
        MicroGItemState.PENDING -> stringResource(R.string.action_pending)
        MicroGItemState.DOWNLOADING -> stringResource(R.string.status_downloading)
        MicroGItemState.INSTALLING -> stringResource(R.string.action_installing)
        MicroGItemState.INSTALLED -> stringResource(R.string.title_installed)
        MicroGItemState.FAILED -> stringResource(R.string.status_failed)
    }
    val statusColor = when (item.state) {
        MicroGItemState.FAILED -> MaterialTheme.colorScheme.error
        MicroGItemState.INSTALLED -> MaterialTheme.colorScheme.primary
        else -> Color.Unspecified
    }
    val detailText = when (item.state) {
        MicroGItemState.DOWNLOADING -> {
            val downloaded = Formatter.formatShortFileSize(
                context,
                (item.totalBytes * item.progress / 100L).coerceAtLeast(0L)
            )
            val total = Formatter.formatShortFileSize(context, item.totalBytes)
            val speed = Formatter.formatShortFileSize(context, item.speed)
            val eta = getETAString(context, item.timeRemaining)
            "$downloaded / $total • $speed/s • $eta"
        }
        else -> Formatter.formatShortFileSize(context, item.totalBytes)
    }
    val trailingIcon = when (item.state) {
        MicroGItemState.INSTALLED -> R.drawable.ic_check
        MicroGItemState.FAILED -> R.drawable.ic_menu_about
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.padding_small),
                vertical = dimensionResource(R.dimen.padding_xsmall)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedAppIcon(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
            iconUrl = item.iconURL,
            inProgress = inProgress,
            progress = item.progress.toFloat()
        )
        Column(
            modifier = Modifier
                .weight(1F)
                .padding(horizontal = dimensionResource(R.dimen.margin_small))
        ) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
            Text(
                text = detailText,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (trailingIcon != null) {
            Icon(
                painter = painterResource(trailingIcon),
                contentDescription = statusText,
                tint = statusColor,
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_default))
            )
        }
    }
}

private fun previewItems(
    companionState: MicroGItemState = MicroGItemState.PENDING,
    companionProgress: Int = 0
): List<MicroGItem> = listOf(
    MicroGItem(
        packageName = "",
        displayName = "microG Companion",
        iconURL = "",
        totalBytes = 1_234_567L,
        state = companionState,
        progress = companionProgress
    )
)

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun MicroGReadyPreview() {
    MicroG(uiState = MicroGUIState(items = previewItems()))
}
