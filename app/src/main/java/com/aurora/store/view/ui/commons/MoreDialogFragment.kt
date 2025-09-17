package com.aurora.store.view.ui.commons

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.Constants
import com.aurora.Constants.URL_TOS
import com.aurora.extensions.browse
import com.aurora.extensions.getStyledAttributeColor
import com.aurora.extensions.navigate
import com.aurora.extensions.setAppTheme
import com.aurora.store.MR
import com.aurora.store.R
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.compose.theme.AuroraTheme
import com.aurora.store.util.Preferences
import com.aurora.store.viewmodel.commons.MoreViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MoreDialogFragment : DialogFragment() {

    private val viewModel: MoreViewModel by viewModels()

    private var primaryColor: Color = Color.White
    private var onPrimaryColor: Color = Color.Black
    private var secondaryColor: Color = Color.White
    private var onSecondaryColor: Color = Color.Black

    private abstract class Option(
        @StringRes open val title: Int,
        @DrawableRes open val icon: Int,
    )

    private data class ViewOption(
        override val title: Int,
        override val icon: Int,
        @IdRes val destinationID: Int
    ) : Option(title, icon)

    private data class ComposeOption(
        override val title: Int,
        override val icon: Int,
        val screen: Screen
    ) : Option(title, icon)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setView(customDialogView(requireContext()))
            .create()
    }

    private fun customDialogView(context: Context): ComposeView {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AuroraTheme {
                    primaryColor =
                        Color(requireContext().getStyledAttributeColor(MR.colorSurface))
                    onPrimaryColor =
                        Color(requireContext().getStyledAttributeColor(MR.colorOnSurface))
                    secondaryColor =
                        Color(requireContext().getStyledAttributeColor(MR.colorSecondaryContainer))
                    onSecondaryColor =
                        Color(requireContext().getStyledAttributeColor(MR.colorOnSecondaryContainer))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = primaryColor)
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            4.dp,
                            Alignment.CenterVertically
                        )
                    ) {
                        AppBar(onBackgroundColor = onPrimaryColor)
                        AccountHeader(
                            backgroundColor = secondaryColor,
                            onBackgroundColor = onSecondaryColor
                        )
                        Column(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 2.dp,
                                        topEnd = 2.dp,
                                        bottomStart = 25.dp,
                                        bottomEnd = 25.dp
                                    )
                                )
                                .background(color = secondaryColor)
                        ) {
                            getOptions().fastForEach { option ->
                                OptionItem(
                                    option = option,
                                    tintColor = onPrimaryColor,
                                    textColor = onSecondaryColor,
                                    onClick = {
                                        when (option) {
                                            is ViewOption -> {
                                                findNavController().navigate(option.destinationID)
                                            }

                                            is ComposeOption -> context.navigate(option.screen)
                                        }
                                    }
                                )
                            }
                        }
                        getExtraOptions().fastForEach { option ->
                            OptionItem(
                                option = option,
                                tintColor = onPrimaryColor,
                                textColor = onPrimaryColor,
                                onClick = {
                                    when (option) {
                                        is ViewOption -> {
                                            findNavController().navigate(option.destinationID)
                                        }

                                        is ComposeOption -> context.navigate(option.screen)
                                    }
                                }
                            )
                        }
                        Footer(onPrimaryColor)
                    }
                }
            }
        }
    }

    @Composable
    fun AppBar(onBackgroundColor: Color) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp, 10.dp, 4.dp, 10.dp)
        ) {
            ThreeStateIconButton(tint = onBackgroundColor)
            Text(
                modifier = Modifier.wrapContentWidth(),
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = onBackgroundColor,
                textAlign = TextAlign.Center
            )

            Image(
                painter = painterResource(id = R.drawable.ic_cancel),
                contentDescription = stringResource(id = R.string.action_cancel),
                modifier = Modifier.clickable {
                    findNavController().navigateUp()
                },
                colorFilter = ColorFilter.tint(onBackgroundColor)
            )
        }
    }

    @Composable
    fun Footer(tintColor: Color) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
        ) {
            TextButton(onClick = { requireContext().browse(Constants.URL_POLICY) }) {
                Text(
                    text = stringResource(id = R.string.privacy_policy_title),
                    fontWeight = FontWeight.Light,
                    color = tintColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(text = "•", color = tintColor)
            TextButton(onClick = { requireContext().browse(URL_TOS) }) {
                Text(
                    text = stringResource(id = R.string.menu_terms),
                    fontWeight = FontWeight.Light,
                    color = tintColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    private fun AccountHeader(backgroundColor: Color, onBackgroundColor: Color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 25.dp,
                        topEnd = 25.dp,
                        bottomStart = 2.dp,
                        bottomEnd = 2.dp
                    )
                )
                .background(color = backgroundColor)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.Start)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(
                            if (viewModel.authProvider.isAnonymous) {
                                R.mipmap.ic_launcher
                            } else {
                                viewModel.authProvider.authData?.userProfile?.artwork?.url
                            }
                        )
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(id = R.string.title_account_manager),
                    placeholder = painterResource(R.drawable.ic_account),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .requiredSize(36.dp)
                        .clip(CircleShape)
                )
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = if (viewModel.authProvider.isAnonymous) {
                            stringResource(R.string.account_anonymous)
                        } else {
                            viewModel.authProvider.authData?.userProfile?.name
                                ?: stringResource(R.string.status_unavailable)
                        },
                        fontWeight = FontWeight.Normal,
                        color = onBackgroundColor,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (viewModel.authProvider.isAnonymous) {
                            stringResource(R.string.account_anonymous_email)
                        } else {
                            viewModel.authProvider.authData?.userProfile?.email
                                ?: stringResource(R.string.status_unavailable)
                        },
                        fontWeight = FontWeight.Light,
                        color = onBackgroundColor,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            OutlinedButton(
                onClick = { requireContext().navigate(Screen.Accounts) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    Color(requireContext().getStyledAttributeColor(androidx.appcompat.R.attr.colorControlHighlight))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.manage_account),
                    color = onBackgroundColor,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    @Composable
    private fun OptionItem(
        option: Option,
        tintColor: Color = Color.Blue,
        textColor: Color = Color.Black,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
        ) {
            Image(
                painter = painterResource(id = option.icon),
                contentDescription = stringResource(id = option.title),
                colorFilter = ColorFilter.tint(color = tintColor),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .requiredSize(23.dp)
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = option.title),
                color = textColor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun ThreeStateIconButton(
        tint: Color = Color.White
    ) {
        var currentState by remember {
            mutableStateOf(
                Preferences.getInteger(
                    requireContext(),
                    Preferences.PREFERENCE_THEME_STYLE
                ).let {
                    State.entries.getOrNull(it)
                } ?: State.Auto
            )
        }

        val iconRes = when (currentState) {
            State.Light -> R.drawable.ic_light
            State.Dark -> R.drawable.ic_dark
            else -> R.drawable.ic_auto
        }

        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.clickable {
                currentState = when (currentState) {
                    State.Light -> State.Dark
                    State.Dark -> State.Auto
                    State.Auto -> State.Light
                }

                Preferences.putInteger(
                    requireContext(),
                    Preferences.PREFERENCE_THEME_STYLE,
                    currentState.value
                )

                setAppTheme(currentState.value)
                findNavController().navigateUp()
            },
            colorFilter = ColorFilter.tint(tint)
        )
    }

    enum class State(val value: Int) {
        Auto(0),
        Light(1),
        Dark(2),
    }

    private fun getOptions(): List<Option> {
        return listOf(
            ViewOption(
                title = R.string.title_apps_games,
                icon = R.drawable.ic_apps,
                destinationID = R.id.appsGamesFragment
            ),
            ComposeOption(
                title = R.string.title_blacklist_manager,
                icon = R.drawable.ic_blacklist,
                screen = Screen.Blacklist
            ),
            ViewOption(
                title = R.string.title_favourites_manager,
                icon = R.drawable.ic_favorite_unchecked,
                destinationID = R.id.favouriteFragment
            ),
            ViewOption(
                title = R.string.title_spoof_manager,
                icon = R.drawable.ic_spoof,
                destinationID = R.id.spoofFragment
            )
        )
    }

    private fun getExtraOptions(): List<Option> {
        return listOf(
            ViewOption(
                title = R.string.title_settings,
                icon = R.drawable.ic_menu_settings,
                destinationID = R.id.settingsFragment
            ),
            ViewOption(
                title = R.string.title_about,
                icon = R.drawable.ic_menu_about,
                destinationID = R.id.aboutFragment
            )
        )
    }
}
