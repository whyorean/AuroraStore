/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.aurora.store.compose.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.account_picker_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.spacing_medium),
                    vertical = dimensionResource(R.dimen.spacing_xsmall)
                )
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.spacing_xsmall))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(vertical = dimensionResource(R.dimen.spacing_xsmall))
            ) {
                accounts.forEachIndexed { index, account ->
                    if (index > 0) {
                        HorizontalDivider()
                    }

                    val suffix = if (account.isDefault) {
                        " · " + stringResource(R.string.account_default)
                    } else {
                        " · " + account.email
                    }

                    val name = if (account.isAnonymous) {
                        stringResource(R.string.account_anonymous)
                    } else {
                        account.displayName ?: account.email
                    }

                    Text(
                        text = name + suffix,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(account) }
                            .padding(
                                horizontal = dimensionResource(R.dimen.spacing_large),
                                vertical = dimensionResource(R.dimen.spacing_small)
                            )
                    )
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
