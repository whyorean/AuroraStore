/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.aurora.extensions.browse
import com.aurora.store.BuildConfig.VERSION_CODE
import com.aurora.store.BuildConfig.VERSION_NAME
import com.aurora.store.R
import com.aurora.store.compose.composables.LinkComposable
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.data.model.Link

@Composable
fun AboutScreen(onNavigateUp: () -> Unit) {
    var shouldShowAboutDialog by rememberSaveable { mutableStateOf(false) }

    if (shouldShowAboutDialog) {
        AboutDialog(onDismiss = { shouldShowAboutDialog = false })
    }

    ScreenContent(
        onNavigateUp = onNavigateUp,
        onAboutAurora = { shouldShowAboutDialog = true }
    )
}

@Composable
private fun ScreenContent(onNavigateUp: () -> Unit = {}, onAboutAurora: () -> Unit = {}) {
    val context = LocalContext.current

    val linkURLS = stringArrayResource(R.array.link_urls)
    val linkTitles = stringArrayResource(R.array.link_titles)
    val linkSummary = stringArrayResource(R.array.link_subtitle)
    val linkIcons = intArrayOf(
        R.drawable.ic_about,
        R.drawable.ic_help,
        R.drawable.ic_xda,
        R.drawable.ic_telegram,
        R.drawable.ic_gitlab,
        R.drawable.ic_fdroid,
        R.drawable.ic_bitcoin_btc,
        R.drawable.ic_bitcoin_bch,
        R.drawable.ic_ethereum_eth,
        R.drawable.ic_bhim,
        R.drawable.ic_paypal,
        R.drawable.ic_libera_pay,
    )

    val links = linkURLS.mapIndexed { index, url ->
        Link(
            id = index,
            title = linkTitles[index],
            subtitle = linkSummary[index],
            url = url,
            icon = linkIcons[index]
        )
    }

    Scaffold(
        topBar = {
            TopAppBarComposable(
                title = stringResource(R.string.title_about),
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xxsmall))
        ) {
            stickyHeader {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    BrandHeader()
                }
            }
            items(items = links, key = { item -> item.id }) { link ->
                LinkComposable(
                    link = link,
                    onClick = {
                        when (link.id) {
                            0 -> onAboutAurora()
                            else -> context.browse(link.url)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.margin_medium)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xxsmall))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(R.mipmap.ic_launcher)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .requiredSize(dimensionResource(R.dimen.icon_size))
                .clip(CircleShape)
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.version, VERSION_NAME, VERSION_CODE),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.made_with_love, String(Character.toChars(0x2764))),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
private fun AboutScreenPreview() {
    ScreenContent()
}
