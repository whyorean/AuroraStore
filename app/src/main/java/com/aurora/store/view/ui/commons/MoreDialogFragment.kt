package com.aurora.store.view.ui.commons

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.aurora.Constants
import com.aurora.Constants.URL_TOS
import com.aurora.extensions.accentColor
import com.aurora.extensions.browse
import com.aurora.store.R
import com.aurora.store.data.providers.AuthProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MoreDialogFragment : DialogFragment() {

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
                val bColor = ColorUtils.setAlphaComponent(LocalContext.current.accentColor(), 20)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color(bColor))
                        .verticalScroll(rememberScrollState())
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
                ) {
                    AppBar()
                    AccountHeader()
                    Column(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 5.dp,
                                    topEnd = 5.dp,
                                    bottomStart = 20.dp,
                                    bottomEnd = 20.dp
                                )
                            )
                            .background(color = Color.White)
                    ) {
                        getOptions().fastForEach { option -> OptionItem(option = option) }
                    }
                    getExtraOptions().fastForEach { option -> OptionItem(option = option) }
                    Footer()
                }
            }
        }
    }

    @Composable
    fun AppBar() {
        Box(contentAlignment = Alignment.CenterStart) {
            IconButton(onClick = { findNavController().navigateUp() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cancel),
                    contentDescription = stringResource(id = R.string.action_cancel),
                    tint = Color.Black
                )
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    fun Footer() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
        ) {
            TextButton(onClick = { requireContext().browse(Constants.URL_POLICY) }) {
                Text(
                    text = stringResource(id = R.string.privacy_policy_title),
                    color = Color.Black
                )
            }
            Text(text = "•")
            TextButton(onClick = { requireContext().browse(URL_TOS) }) {
                Text(
                    text = stringResource(id = R.string.menu_terms),
                    color = Color.Black
                )
            }
        }
    }

    @Composable
    private fun AccountHeader() {
        val authData = AuthProvider.with(LocalContext.current).getAuthData()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 5.dp,
                        bottomEnd = 5.dp
                    )
                )
                .background(color = Color.White)
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
                        .data(if (authData.isAnonymous) R.mipmap.ic_launcher else authData.userProfile?.artwork?.url)
                        .placeholder(R.drawable.ic_account)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(id = R.string.title_account_manager),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .clip(CircleShape)
                )
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = if (authData.isAnonymous) "anonymous" else authData.userProfile!!.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (authData.isAnonymous) "anonymous@gmail.com" else authData.userProfile!!.email,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
            OutlinedButton(
                onClick = { findNavController().navigate(R.id.accountFragment) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.manage_account),
                    color = Color.Black
                )
            }
        }
    }

    @Composable
    private fun OptionItem(option: Option) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { findNavController().navigate(option.destinationID) }
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
        ) {
            Image(
                painter = painterResource(id = option.icon),
                contentDescription = stringResource(id = option.title),
                colorFilter = ColorFilter.tint(color = Color.Black),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .requiredSize(25.dp)
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = option.title),
                color = Color.Black,
                fontSize = 16.sp,
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
                title = R.string.title_apps_sale,
                icon = R.drawable.ic_sale,
                destinationID = R.id.appSalesFragment
            ),
            Option(
                title = R.string.title_blacklist_manager,
                icon = R.drawable.ic_blacklist,
                destinationID = R.id.blacklistFragment
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
