/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.Constants.URL_POLICY
import com.aurora.Constants.URL_TOS
import com.aurora.extensions.browse
import com.aurora.extensions.setAppTheme
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.compose.composable.OptionListItem
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.model.ComposeOption
import com.aurora.store.data.model.Option
import com.aurora.store.data.model.ThemeState
import com.aurora.store.data.model.ViewOption
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_THEME_STYLE

@Composable
fun MoreDialog(
    avatar: Any = R.mipmap.ic_launcher,
    name: String = stringResource(R.string.account_anonymous),
    email: String = stringResource(R.string.account_anonymous_email),
    onNavigateTo: (screen: NavKey) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val activity = LocalActivity.current
    val context = LocalContext.current

    val options = listOf(
        ComposeOption(
            title = R.string.title_apps_games,
            icon = R.drawable.ic_apps,
            screen = Screen.Installed
        ),
        ComposeOption(
            title = R.string.title_blacklist_manager,
            icon = R.drawable.ic_blacklist,
            screen = Screen.Blacklist
        ),
        ComposeOption(
            title = R.string.title_favourites_manager,
            icon = R.drawable.ic_favorite_unchecked,
            screen = Screen.Favourite
        ),
        ComposeOption(
            title = R.string.title_spoof_manager,
            icon = R.drawable.ic_spoof,
            screen = Screen.Spoof
        )
    )
    val extraOptions = listOf(
        ViewOption(
            title = R.string.title_settings,
            icon = R.drawable.ic_menu_settings,
            destinationID = R.id.settingsFragment
        ),
        ComposeOption(
            title = R.string.title_about,
            icon = R.drawable.ic_menu_about,
            screen = Screen.About
        )
    )

    fun onOptionClicked(option: Option) {
        when (option) {
            is ViewOption -> {
                val intent = NavDeepLinkBuilder(context)
                    .setGraph(R.navigation.mobile_navigation)
                    .setDestination(option.destinationID)
                    .setComponentName(MainActivity::class.java)
                    .createTaskStackBuilder()
                    .intents
                    .first()

                activity?.startActivity(intent)
            }

            is ComposeOption -> onNavigateTo(option.screen)
        }

        onDismiss()
    }

    @Composable
    fun ThemeStateIconButton() {
        var themeState by remember {
            mutableStateOf(
                ThemeState.entries[Preferences.getInteger(context, PREFERENCE_THEME_STYLE)]
            )
        }

        IconButton(
            onClick = {
                themeState = when (themeState) {
                    ThemeState.AUTO -> ThemeState.LIGHT
                    ThemeState.LIGHT -> ThemeState.DARK
                    ThemeState.DARK -> ThemeState.AUTO
                }
                Preferences.putInteger(context, PREFERENCE_THEME_STYLE, themeState.ordinal)
                setAppTheme(themeState.ordinal)
            }
        ) {
            Icon(
                painter = painterResource(id = themeState.icon),
                contentDescription = null
            )
        }
    }

    @Composable
    fun AppBar() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    dimensionResource(R.dimen.padding_xsmall),
                    dimensionResource(R.dimen.padding_medium),
                    dimensionResource(R.dimen.padding_xsmall),
                    dimensionResource(R.dimen.padding_medium)
                )
        ) {
            ThemeStateIconButton()

            Text(
                modifier = Modifier.wrapContentWidth(),
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cancel),
                    contentDescription = stringResource(id = R.string.action_cancel)
                )
            }
        }
    }

    @Composable
    fun AccountHeader() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = dimensionResource(R.dimen.radius_large),
                        topEnd = dimensionResource(R.dimen.radius_large),
                        bottomStart = dimensionResource(R.dimen.radius_xxsmall),
                        bottomEnd = dimensionResource(R.dimen.radius_xxsmall)
                    )
                )
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(dimensionResource(R.dimen.padding_large)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_large)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.margin_large),
                    Alignment.Start
                )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(id = R.string.title_account_manager),
                    placeholder = painterResource(R.drawable.ic_account),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .requiredSize(dimensionResource(R.dimen.icon_size_category))
                        .clip(CircleShape)
                )
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            OutlinedButton(
                onClick = { onNavigateTo(Screen.Accounts) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.radius_normal)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.manage_account),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    fun Footer() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.margin_xxsmall),
                Alignment.CenterHorizontally
            )
        ) {
            TextButton(onClick = { context.browse(URL_POLICY) }) {
                Text(
                    text = stringResource(id = R.string.privacy_policy_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(text = "•")
            TextButton(onClick = { context.browse(URL_TOS) }) {
                Text(
                    text = stringResource(id = R.string.menu_terms),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_large))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(R.dimen.padding_medium)),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.margin_xsmall),
                    Alignment.CenterVertically
                )
            ) {
                AppBar()
                AccountHeader()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                topStart = dimensionResource(R.dimen.radius_xxsmall),
                                topEnd = dimensionResource(R.dimen.radius_xxsmall),
                                bottomStart = dimensionResource(R.dimen.radius_large),
                                bottomEnd = dimensionResource(R.dimen.radius_large)
                            )
                        )
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    options.fastForEach { option ->
                        OptionListItem(
                            option = option,
                            onClick = { onOptionClicked(option) }
                        )
                    }
                }

                extraOptions.fastForEach { option ->
                    OptionListItem(
                        option = option,
                        onClick = { onOptionClicked(option) }
                    )
                }

                Footer()
            }
        }
    }
}

@Preview
@Composable
private fun MoreDialogPreview() {
    PreviewTemplate {
        MoreDialog()
    }
}
