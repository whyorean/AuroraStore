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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.aurora.Constants
import com.aurora.Constants.URL_TOS
import com.aurora.extensions.accentColor
import com.aurora.extensions.backgroundColor
import com.aurora.extensions.browse
import com.aurora.extensions.darkenColor
import com.aurora.extensions.getStyledAttributeColor
import com.aurora.extensions.lightenColor
import com.aurora.store.R
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.view.theme.AuroraTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MoreDialogFragment : DialogFragment() {

    @Inject
    lateinit var authProvider: AuthProvider

    private var primaryColor: Color = Color.White
    private var onPrimaryColor: Color = Color.Black
    private var secondaryColor: Color = Color.White
    private var onSecondaryColor: Color = Color.Black

    private data class Option(
        @StringRes val title: Int,
        @DrawableRes val icon: Int,
        @IdRes val destinationID: Int
    )

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
                    if (isSystemInDarkTheme()) {
                        primaryColor = Color(requireContext().backgroundColor())
                        onPrimaryColor = Color(lightenColor(primaryColor.toArgb(), 0.85f))
                        secondaryColor = Color(darkenColor(primaryColor.toArgb(), 0.75f))
                        onSecondaryColor = Color(lightenColor(secondaryColor.toArgb(), 0.85f))
                    } else {
                        primaryColor = Color(lightenColor(requireContext().accentColor(), 0.85f))
                        onPrimaryColor = Color(darkenColor(primaryColor.toArgb(), 0.15f))
                        secondaryColor = Color(lightenColor(primaryColor.toArgb(), 0.75f))
                        onSecondaryColor = Color(darkenColor(secondaryColor.toArgb(), 0.15f))
                    }

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
                        AppBar(
                            backgroundColor = primaryColor,
                            onBackgroundColor = onPrimaryColor
                        )
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
                                    textColor = onSecondaryColor
                                )
                            }
                        }
                        getExtraOptions().fastForEach { option ->
                            OptionItem(
                                option = option,
                                tintColor = onPrimaryColor,
                                textColor = onPrimaryColor
                            )
                        }
                        Footer(onPrimaryColor)
                    }
                }
            }
        }
    }

    @Composable
    fun AppBar(backgroundColor: Color = Color.Transparent, onBackgroundColor: Color) {
        Box(contentAlignment = Alignment.CenterEnd) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = onBackgroundColor,
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { findNavController().navigateUp() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cancel),
                    contentDescription = stringResource(id = R.string.action_cancel),
                    tint = onBackgroundColor
                )
            }
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
                    color = tintColor
                )
            }
            Text(text = "•", color = tintColor)
            TextButton(onClick = { requireContext().browse(URL_TOS) }) {
                Text(
                    text = stringResource(id = R.string.menu_terms),
                    fontWeight = FontWeight.Light,
                    color = tintColor
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
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(if (authProvider.isAnonymous) R.mipmap.ic_launcher else authProvider.authData?.userProfile?.artwork?.url)
                        .placeholder(R.drawable.ic_account)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(id = R.string.title_account_manager),
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
                        text = if (authProvider.isAnonymous) "Anonymous" else authProvider.authData!!.userProfile!!.name,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = onBackgroundColor
                    )
                    Text(
                        text = if (authProvider.isAnonymous) "anonymous@gmail.com" else authProvider.authData!!.userProfile!!.email,
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = onBackgroundColor
                    )
                }
            }
            OutlinedButton(
                onClick = { findNavController().navigate(R.id.accountFragment) },
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
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    @Composable
    private fun OptionItem(
        option: Option,
        tintColor: Color = Color.Blue,
        textColor: Color = Color.Black
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { findNavController().navigate(option.destinationID) }
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
                fontSize = 15.sp,
            )
        }
    }

    private fun getOptions(): List<Option> {
        return listOf(
            Option(
                title = R.string.title_apps_games,
                icon = R.drawable.ic_apps,
                destinationID = R.id.appsGamesFragment
            ),
            Option(
                title = R.string.title_blacklist_manager,
                icon = R.drawable.ic_blacklist,
                destinationID = R.id.blacklistFragment
            ),
            Option(
                title = R.string.title_favourites_manager,
                icon = R.drawable.ic_favorite_unchecked,
                destinationID = R.id.favouriteFragment
            ),
            Option(
                title = R.string.title_spoof_manager,
                icon = R.drawable.ic_spoof,
                destinationID = R.id.spoofFragment
            )
        )
    }

    private fun getExtraOptions(): List<Option> {
        return listOf(
            Option(
                title = R.string.title_settings,
                icon = R.drawable.ic_menu_settings,
                destinationID = R.id.settingsFragment
            ),
            Option(
                title = R.string.title_about,
                icon = R.drawable.ic_menu_about,
                destinationID = R.id.aboutFragment
            )
        )
    }
}
