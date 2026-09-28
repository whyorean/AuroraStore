/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.menu

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.aurora.Constants
import com.aurora.store.R
import com.aurora.store.compose.composable.ExpressiveCheckableMenuItem
import com.aurora.store.compose.composable.ExpressiveMenuIcon
import com.aurora.store.compose.composable.ExpressiveMenuGroup
import com.aurora.store.compose.composable.ExpressiveMenuItem
import com.aurora.store.compose.composable.ExpressiveMenuPopup
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.AppState
import com.aurora.store.util.PackageUtil

/**
 * Menu for the app details screen
 * @param modifier The modifier to be applied to the composable
 * @param onMenuItemClicked Callback when a menu item has been clicked
 * @see MenuItem
 */
@Composable
fun AppDetailsMenu(
    modifier: Modifier = Modifier,
    state: AppState = AppState.Unavailable,
    isFavorite: Boolean = false,
    isExpanded: Boolean = false,
    canManualDownload: Boolean = true,
    canUseOtherAccount: Boolean = false,
    onMenuItemClicked: (menuItem: MenuItem) -> Unit = {}
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(isExpanded) }
    val interactionSources = remember { List(3) { MutableInteractionSource() } }
    val leadingShapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
    val middleShapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
    val trailingShapes = ButtonGroupDefaults.connectedTrailingButtonShapes()

    fun onClick(menuItem: MenuItem) {
        onMenuItemClicked(menuItem)
        expanded = false
    }

    Box(modifier = modifier) {
        ButtonGroup(
            modifier = Modifier.width(164.dp),
            overflowIndicator = { menuState ->
                ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
            },
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                ButtonGroupDefaults.ConnectedSpaceBetween
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
        val favoriteModifier = Modifier.weight(1.15f).animateWidth(interactionSources[0])
        val shareModifier = Modifier.weight(1f).animateWidth(interactionSources[1])
        val overflowModifier = Modifier.weight(0.9f).animateWidth(interactionSources[2])

        customItem(
            buttonGroupContent = {
                FilledTonalIconToggleButton(
                    checked = isFavorite,
                    onCheckedChange = { onClick(MenuItem.FAVORITE) },
                    modifier = favoriteModifier,
                    interactionSource = interactionSources[0],
                    shapes = IconButtonDefaults.toggleableShapes(
                        shape = leadingShapes.shape,
                        pressedShape = leadingShapes.pressedShape,
                        checkedShape = leadingShapes.checkedShape
                    )
                ) {
                    Icon(
                        painter = if (isFavorite) {
                            painterResource(R.drawable.ic_favorite_checked)
                        } else {
                            painterResource(R.drawable.ic_favorite_unchecked)
                        },
                        contentDescription = stringResource(R.string.action_favourite)
                    )
                }
            },
            menuContent = { menuState ->
                ExpressiveCheckableMenuItem(
                    index = 0,
                    count = 2,
                    checked = isFavorite,
                    text = {
                        Text(
                            text = if (isFavorite) {
                                stringResource(R.string.action_remove_favourite)
                            } else {
                                stringResource(R.string.action_favourite)
                            }
                        )
                    },
                    onCheckedChange = {
                        menuState.dismiss()
                        onClick(MenuItem.FAVORITE)
                    },
                    leadingIcon = {
                        ExpressiveMenuIcon(painterResource(R.drawable.ic_favorite_unchecked))
                    },
                    checkedLeadingIcon = {
                        ExpressiveMenuIcon(painterResource(R.drawable.ic_favorite_checked))
                    }
                )
            }
        )

        customItem(
            buttonGroupContent = {
                FilledTonalIconButton(
                    onClick = { onClick(MenuItem.SHARE) },
                    modifier = shareModifier,
                    interactionSource = interactionSources[1],
                    shapes = IconButtonDefaults.shapes(
                        shape = middleShapes.shape,
                        pressedShape = middleShapes.pressedShape
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = stringResource(R.string.action_share)
                    )
                }
            },
            menuContent = { menuState ->
                ExpressiveMenuItem(
                    index = 1,
                    count = 2,
                    text = { Text(text = stringResource(R.string.action_share)) },
                    leadingIcon = {
                        ExpressiveMenuIcon(painterResource(R.drawable.ic_share))
                    },
                    onClick = {
                        menuState.dismiss()
                        onClick(MenuItem.SHARE)
                    }
                )
            }
        )

        customItem(
            buttonGroupContent = {
                FilledTonalIconButton(
                    onClick = { expanded = true },
                    modifier = overflowModifier,
                    interactionSource = interactionSources[2],
                    shapes = IconButtonDefaults.shapes(
                        shape = trailingShapes.shape,
                        pressedShape = trailingShapes.pressedShape
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.menu)
                    )
                }
            },
            menuContent = { menuState ->
                MenuItems(
                    state = state,
                    canManualDownload = canManualDownload,
                    canUseOtherAccount = canUseOtherAccount,
                    onClick = { item ->
                        menuState.dismiss()
                        onClick(item)
                    }
                )
            }
        )
        }
        ExpressiveMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            ExpressiveMenuGroup(index = 0, count = 1) {
                MenuItems(
                    state = state,
                    canManualDownload = canManualDownload,
                    canUseOtherAccount = canUseOtherAccount,
                    onClick = ::onClick
                )
            }
        }
    }
}

@Composable
private fun MenuItems(
    state: AppState,
    canManualDownload: Boolean,
    canUseOtherAccount: Boolean,
    onClick: (MenuItem) -> Unit
) {
    val context = LocalContext.current
    ExpressiveMenuItem(
        index = 0,
        count = 5,
        text = { Text(text = stringResource(R.string.title_manual_download)) },
        onClick = { onClick(MenuItem.MANUAL_DOWNLOAD) },
        leadingIcon = {
            ExpressiveMenuIcon(painterResource(R.drawable.ic_apk_install))
        },
        enabled = canManualDownload && !state.inProgress()
    )
    ExpressiveMenuItem(
        index = 1,
        count = 5,
        text = { Text(text = stringResource(R.string.action_switch_account)) },
        onClick = { onClick(MenuItem.INSTALL_OTHER_ACCOUNT) },
        leadingIcon = {
            ExpressiveMenuIcon(painterResource(R.drawable.ic_account))
        },
        enabled = canUseOtherAccount && !state.inProgress()
    )
    ExpressiveMenuItem(
        index = 2,
        count = 5,
        text = { Text(text = stringResource(R.string.action_info)) },
        onClick = { onClick(MenuItem.APP_INFO) },
        leadingIcon = {
            ExpressiveMenuIcon(painterResource(R.drawable.ic_menu_about))
        },
        enabled = state is AppState.Installed || state is AppState.Updatable
    )
    ExpressiveMenuItem(
        index = 3,
        count = 5,
        text = { Text(text = stringResource(R.string.action_home_screen)) },
        onClick = { onClick(MenuItem.ADD_TO_HOME) },
        leadingIcon = {
            ExpressiveMenuIcon(painterResource(R.drawable.ic_apps))
        },
        enabled = state is AppState.Installed || state is AppState.Updatable
    )
    ExpressiveMenuItem(
        index = 4,
        count = 5,
        text = { Text(text = stringResource(R.string.action_view_on_play)) },
        onClick = { onClick(MenuItem.PLAY_STORE) },
        leadingIcon = {
            ExpressiveMenuIcon(painterResource(R.drawable.ic_google))
        },
        enabled = PackageUtil.isInstalled(context, Constants.PACKAGE_NAME_PLAY_STORE)
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun AppDetailsMenuPreview() {
    TopAppBar(
        actions = {
            AppDetailsMenu(isFavorite = true, isExpanded = true)
        }
    )
}
