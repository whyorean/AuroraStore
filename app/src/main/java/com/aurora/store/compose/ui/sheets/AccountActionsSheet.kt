/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.aurora.store.compose.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.aurora.store.R
import com.aurora.store.data.room.account.Account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountActionsSheet(
    account: Account,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val name = if (account.isAnonymous) {
        stringResource(R.string.account_anonymous)
    } else {
        account.displayName ?: account.email
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(R.dimen.spacing_small))
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.spacing_large)
                )
            )

            Text(
                text = account.email,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.spacing_large)
                )
            )

            if (!account.isDefault) {
                ActionRow(
                    labelRes = R.string.account_set_default,
                    onClick = onSetDefault
                )
            }

            ActionRow(
                labelRes = R.string.account_remove,
                onClick = onRemove
            )

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun ActionRow(labelRes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_medium)
            ),
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.spacing_medium)
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
