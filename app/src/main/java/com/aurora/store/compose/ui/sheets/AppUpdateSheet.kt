/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.extensions.openInfo
import com.aurora.extensions.toast
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.event.BusEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.room.account.Account
import com.aurora.store.data.room.update.Update
import com.aurora.store.viewmodel.sheets.AppUpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateSheet(
    update: Update,
    onDismiss: () -> Unit,
    onNavigateTo: (Destination) -> Unit = {},
    viewModel: AppUpdateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isBlacklisted = viewModel.blacklistProvider.isBlacklisted(update.packageName)
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var showAccounts by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            AppHeader(
                update = update,
                onShowDetails = {
                    onNavigateTo(Destination.AppDetails(update.packageName))
                    onDismiss()
                }
            )

            if (update.changelog.isNotEmpty()) {
                ChangelogSection(html = update.changelog)
            }

            HorizontalDivider(
                modifier = Modifier.padding(
                    vertical = dimensionResource(R.dimen.spacing_small)
                )
            )

            Item(
                label = stringResource(R.string.action_ignore_all),
                onClick = {
                    viewModel.ignoreAllUpdates(update.packageName)
                    onDismiss()
                }
            )

            Item(
                label = stringResource(
                    R.string.action_ignore_version,
                    update.versionName
                ),
                onClick = {
                    viewModel.ignoreVersion(update.packageName, update.versionCode)
                    onDismiss()
                }
            )

            // Per-app update override: only meaningful with more than one account. The primary
            // update button keeps using the default (or the app's existing binding).
            if (accounts.size > 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        vertical = dimensionResource(R.dimen.spacing_small)
                    )
                )

                AccountAccordion(
                    expanded = showAccounts,
                    onToggle = { showAccounts = !showAccounts },
                    accounts = accounts,
                    onSelect = { account ->
                        viewModel.updateWithAccount(update, account.id)
                        onDismiss()
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(
                    vertical = dimensionResource(R.dimen.spacing_small)
                )
            )

            Item(
                label = stringResource(
                    if (isBlacklisted) {
                        R.string.action_whitelist
                    } else {
                        R.string.action_blacklist_add
                    }
                ),
                onClick = {
                    if (isBlacklisted) {
                        viewModel.blacklistProvider.whitelist(update.packageName)
                        context.toast(R.string.toast_apk_whitelisted)
                    } else {
                        viewModel.blacklistProvider.blacklist(update.packageName)
                        context.toast(R.string.toast_apk_blacklisted)
                    }
                    AuroraApp.events.send(BusEvent.Blacklisted(update.packageName))
                    onDismiss()
                }
            )

            Item(
                label = stringResource(R.string.action_uninstall),
                onClick = {
                    AppInstaller.uninstall(context, update.packageName)
                    onDismiss()
                }
            )

            Item(
                label = stringResource(R.string.action_info),
                onClick = {
                    context.openInfo(update.packageName)
                    onDismiss()
                }
            )

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun AppHeader(update: Update, onShowDetails: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_xsmall)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(update.iconURL)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = update.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = update.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.version, update.versionName, update.versionCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledTonalButton(onClick = onShowDetails) {
            Text(stringResource(R.string.updates_app_details))
        }
    }
}

@Composable
private fun Item(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_xsmall))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_small)
            )
    )
}

/**
 * Expandable "update using another account" row. Tapping the header toggles a tinted sub-menu
 * listing the available accounts, so it reads as a nested section rather than a sibling action.
 */
@Composable
private fun AccountAccordion(
    expanded: Boolean,
    onToggle: () -> Unit,
    accounts: List<Account>,
    onSelect: (Account) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_xsmall))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
            .clickable(onClick = onToggle)
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.action_update_other_account),
            style = MaterialTheme.typography.bodyLarge
        )
        Icon(
            painter = painterResource(
                if (expanded) R.drawable.ic_keyboard_arrow_up else R.drawable.ic_keyboard_arrow_down
            ),
            contentDescription = null
        )
    }

    Spacer(Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
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
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.spacing_large)
                        )
                    )
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
    }
}

@Composable
private fun ChangelogSection(html: String) {
    Text(
        text = stringResource(R.string.details_changelog),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(
            horizontal = dimensionResource(R.dimen.spacing_medium),
            vertical = dimensionResource(R.dimen.spacing_xsmall)
        )
    )

    Text(
        text = AnnotatedString.fromHtml(html),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            horizontal = dimensionResource(R.dimen.spacing_medium),
            vertical = dimensionResource(R.dimen.spacing_xsmall)
        )
    )
}
