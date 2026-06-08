/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.extensions.copyToClipBoard
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import kotlinx.coroutines.android.awaitFrame

/**
 * TV-styled, remote-focusable equivalent of the phone's `InstallErrorSheet`.
 *
 * A modal bottom sheet is unusable with a D-pad, so install failures surface here as a centered
 * [Dialog] with tv-material controls. Back/outside dismisses (default [Dialog] behaviour); the OK
 * button grabs focus on first frame so the remote lands on an actionable control. Mirrors the phone
 * actions: copy the full error to the clipboard, or dismiss. The phone's "buy" shortcut is omitted
 * because TV is anonymous-login-only (a paid-app purchase can never apply here).
 */
@Composable
fun InstallErrorDialog(app: App, error: String?, extra: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val okFocusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(dimensionResource(R.dimen.tv_dialog_width)),
            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large))
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_xlarge)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_large)
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(app.iconArtwork.url)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.icon_size_small))
                            .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.title_installer),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_small),
                        Alignment.End
                    )
                ) {
                    Button(
                        onClick = {
                            context.copyToClipBoard(
                                listOfNotNull(error, extra).joinToString("\n\n")
                            )
                            context.toast(R.string.toast_clipboard_copied)
                        }
                    ) {
                        Text(text = stringResource(R.string.action_copy))
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(okFocusRequester)
                    ) {
                        Text(text = stringResource(R.string.action_ok))
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        awaitFrame()
        runCatching { okFocusRequester.requestFocus() }
    }
}
