/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.dispenser

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aurora.store.R
import kotlinx.coroutines.android.awaitFrame

/**
 * Dialog for adding a new token dispenser
 * @param onAdd Callback on adding a new dispenser
 * @param onDismiss Callback on dismiss
 */
@Composable
fun InputDispenserDialog(onAdd: (url: String) -> Unit = {}, onDismiss: () -> Unit = {}) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var url by remember { mutableStateOf(TextFieldValue()) }

    LaunchedEffect(focusRequester) {
        awaitFrame()
        focusRequester.requestFocus()
    }

    AlertDialog(
        title = { Text(text = stringResource(R.string.add_dispenser_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
            ) {
                Text(text = stringResource(R.string.add_dispenser_summary))
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    value = url,
                    placeholder = { Text(text = stringResource(R.string.add_dispenser_hint)) },
                    onValueChange = { url = it },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onAdd(url.text) },
                enabled = Patterns.WEB_URL.matcher(url.text).matches()
            ) {
                Text(text = stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
private fun InputDispenserDialogPreview() {
    InputDispenserDialog()
}
