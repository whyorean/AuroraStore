/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.aurora.store.compose.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.aurora.store.R
import com.aurora.store.data.room.account.Account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    accounts: List<Account>,
    onSelect: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_medium))
        ) {
            Text(
                text = stringResource(R.string.account_picker_title),
                style = MaterialTheme.typography.titleMedium
            )
            LazyColumn {
                items(accounts, key = { it.id }) { account ->
                    val suffix = if (account.isDefault) {
                        " · " + stringResource(R.string.account_default)
                    } else {
                        ""
                    }
                    val name = if (account.isAnonymous) {
                        stringResource(R.string.account_anonymous)
                    } else {
                        account.displayName ?: account.email
                    }
                    Text(
                        text = name + suffix,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(account) }
                            .padding(dimensionResource(R.dimen.spacing_small))
                    )
                }
            }
        }
    }
}
