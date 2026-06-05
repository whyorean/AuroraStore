/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.accounts

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.Constants.URL_DISCLAIMER
import com.aurora.Constants.URL_LICENSE
import com.aurora.Constants.URL_TOS
import com.aurora.extensions.browse
import com.aurora.store.R
import com.aurora.store.compose.composable.AccountListItem
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.compose.ui.commons.LoadingDialog
import com.aurora.store.compose.ui.sheets.AccountActionsSheet
import com.aurora.store.data.room.account.Account
import com.aurora.store.util.RestartUtil
import com.aurora.store.viewmodel.accounts.AccountsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigateTo: (Destination) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var accountToRemove by remember { mutableStateOf<Account?>(null) }
    var accountToSetDefault by remember { mutableStateOf<Account?>(null) }
    var isSwitchingDefault by remember { mutableStateOf(false) }
    var isAddingAccount by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.switched.collect { ok ->
            if (ok) {
                RestartUtil.restart(context)
            } else {
                isSwitchingDefault = false
                Toast.makeText(
                    context,
                    context.getString(R.string.account_switch_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.removeResult.collect { result ->
            when (result) {
                AccountsViewModel.RemoveResult.LOGGED_OUT -> onNavigateTo(Destination.Splash())
                AccountsViewModel.RemoveResult.RESTART -> RestartUtil.restart(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.addResult.collect { added ->
            isAddingAccount = false
            if (!added) {
                Toast.makeText(
                    context,
                    context.getString(R.string.account_add_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    ScreenContent(
        accounts = accounts,
        onAccountClick = { selectedAccount = it },
        onAddAccount = { showAddSheet = true }
    )

    selectedAccount?.let { account ->
        AccountActionsSheet(
            account = account,
            onSetDefault = {
                selectedAccount = null
                accountToSetDefault = account
            },
            onRemove = {
                selectedAccount = null
                accountToRemove = account
            },
            onDismiss = { selectedAccount = null }
        )
    }

    accountToSetDefault?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToSetDefault = null },
            title = { Text(text = stringResource(R.string.account_set_default_title)) },
            text = { Text(text = stringResource(R.string.account_set_default_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountToSetDefault = null
                        isSwitchingDefault = true
                        viewModel.setDefault(account)
                    }
                ) {
                    Text(text = stringResource(R.string.action_restart))
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToSetDefault = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (isSwitchingDefault) {
        LoadingDialog(message = stringResource(R.string.account_switching))
    }

    if (isAddingAccount) {
        LoadingDialog(message = stringResource(R.string.account_adding))
    }

    accountToRemove?.let { account ->
        val isLast = accounts.size <= 1
        val name = if (account.isAnonymous) {
            stringResource(R.string.account_anonymous)
        } else {
            account.displayName ?: account.email
        }
        AlertDialog(
            onDismissRequest = { accountToRemove = null },
            title = { Text(text = stringResource(R.string.account_remove_title)) },
            text = {
                Text(
                    text = if (isLast) {
                        stringResource(R.string.account_remove_last_message)
                    } else {
                        stringResource(R.string.account_remove_message, name)
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountToRemove = null
                        viewModel.remove(account)
                    }
                ) {
                    Text(text = stringResource(R.string.account_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToRemove = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showAddSheet) {
        val deviceEmails by produceState(initialValue = emptyList()) {
            value = viewModel.discoverableEmails()
        }

        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(R.dimen.spacing_small))
            ) {
                Text(
                    text = stringResource(R.string.account_add),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(R.dimen.spacing_large),
                        vertical = dimensionResource(R.dimen.spacing_small)
                    )
                )

                if (deviceEmails.isNotEmpty()) {
                    SectionLabel(text = stringResource(R.string.account_add_device))
                    deviceEmails.forEach { email ->
                        AddOptionRow(
                            iconRes = R.drawable.ic_account,
                            label = email,
                            onClick = {
                                showAddSheet = false
                                isAddingAccount = true
                                viewModel.addSystemAccount(email)
                            }
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            vertical = dimensionResource(R.dimen.spacing_xsmall)
                        )
                    )
                }

                AddOptionRow(
                    iconRes = R.drawable.ic_google,
                    label = stringResource(R.string.account_add_google),
                    onClick = {
                        showAddSheet = false
                        onNavigateTo(Destination.GoogleLogin(addAccount = true))
                    }
                )

                if (accounts.none { it.isAnonymous }) {
                    AddOptionRow(
                        iconRes = R.drawable.ic_anonymous,
                        label = stringResource(R.string.account_add_anonymous),
                        onClick = {
                            showAddSheet = false
                            isAddingAccount = true
                            viewModel.addAnonymousAccount()
                        }
                    )
                }

                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenContent(
    accounts: List<Account> = emptyList(),
    onAccountClick: (Account) -> Unit = {},
    onAddAccount: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_account_manager),
                actions = { OverflowMenu() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AccountsHeader(count = accounts.size, onAddAccount = onAddAccount)
            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(items = accounts, key = { _, item -> item.id }) { index, account ->
                    AccountListItem(
                        account = account,
                        onClick = { onAccountClick(account) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            horizontal = dimensionResource(R.dimen.spacing_large),
            vertical = dimensionResource(R.dimen.spacing_xsmall)
        )
    )
}

@Composable
private fun AddOptionRow(iconRes: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_medium)
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AccountsHeader(count: Int, onAddAccount: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = pluralStringResource(R.plurals.account_count, count, count),
            style = MaterialTheme.typography.titleSmall
        )
        FilledTonalButton(onClick = onAddAccount) {
            Text(text = stringResource(R.string.account_add))
        }
    }
}

@Composable
private fun OverflowMenu() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val links = mapOf(
        R.string.menu_terms to URL_TOS,
        R.string.menu_disclaimer to URL_DISCLAIMER,
        R.string.menu_license to URL_LICENSE
    )

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.menu)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            links.forEach { (label, url) ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(label)) },
                    onClick = {
                        expanded = false
                        context.browse(url)
                    }
                )
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun AccountsScreenPreview() {
    ScreenContent()
}
