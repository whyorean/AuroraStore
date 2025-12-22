/*
 * SPDX-FileCopyrightText: 2026 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.update.Update

@Composable
fun UpdateListItem(
    modifier: Modifier = Modifier,
    update: Update,
    download: Download? = null,
    onClick: () -> Unit = {},
    onUpdate: () -> Unit = {},
    onCancel: () -> Unit = {},
    isExpanded: Boolean = false
) {
    val isDownloading = download?.isRunning ?: false
    val size = Formatter.formatShortFileSize(LocalContext.current, update.size)

    var isVisible by remember { mutableStateOf(true) }
    var isExpanded by remember { mutableStateOf(isExpanded) }

    AnimatedVisibility(visible = isVisible, exit = shrinkVertically() + fadeOut()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = dimensionResource(R.dimen.padding_medium),
                    vertical = dimensionResource(R.dimen.padding_small)
                )
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.weight(1F)) {
                    AnimatedAppIcon(
                        modifier = Modifier.requiredSize(
                            dimensionResource(R.dimen.icon_size_medium)
                        ),
                        iconUrl = update.iconURL,
                        inProgress = download?.isRunning == true,
                        progress = download?.progress?.toFloat() ?: 0F
                    )
                    Column(
                        modifier = Modifier
                            .padding(horizontal = dimensionResource(R.dimen.margin_small))
                    ) {
                        Text(
                            text = update.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$size • ${update.updatedOn}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                R.string.version,
                                update.versionName,
                                update.versionCode
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_down),
                        contentDescription = stringResource(R.string.expand)
                    )
                }

                FilledTonalButton(onClick = if (isDownloading) onCancel else onUpdate) {
                    Text(
                        text = when {
                            isDownloading -> stringResource(R.string.action_cancel)
                            else -> stringResource(R.string.action_update)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Box(
                    modifier = Modifier
                        .padding(vertical = dimensionResource(R.dimen.margin_small))
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                        .background(color = MaterialTheme.colorScheme.secondaryContainer)
                        .padding(dimensionResource(R.dimen.padding_medium))
                ) {
                    Text(
                        text = if (update.changelog.isBlank()) {
                            AnnotatedString(
                                text = stringResource(R.string.details_changelog_unavailable)
                            )
                        } else {
                            AnnotatedString.fromHtml(htmlString = update.changelog)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateListItemPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    val context = LocalContext.current
    PreviewTemplate {
        UpdateListItem(
            update = Update.fromApp(context, app),
            isExpanded = true
        )
    }
}
