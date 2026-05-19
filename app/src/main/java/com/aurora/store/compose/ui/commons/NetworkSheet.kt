/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aurora.extensions.isQAndAbove
import com.aurora.store.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkSheet() {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                .padding(bottom = dimensionResource(R.dimen.spacing_large)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_network),
                contentDescription = null,
                modifier = Modifier.requiredSize(48.dp)
            )
            Spacer(Modifier.padding(top = dimensionResource(R.dimen.spacing_large)))
            Text(
                text = stringResource(R.string.title_no_network),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.padding(top = dimensionResource(R.dimen.spacing_small)))
            Text(
                text = stringResource(R.string.check_connectivity),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.padding(top = dimensionResource(R.dimen.spacing_xlarge)))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    try {
                        if (isQAndAbove) {
                            context.startActivity(
                                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                            )
                        } else {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                        }
                    } catch (e: ActivityNotFoundException) {
                        Log.i("NetworkSheet", "Unable to launch network settings")
                        try {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        } catch (_: Exception) { }
                    }
                }
            ) {
                Text(stringResource(R.string.action_check))
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
