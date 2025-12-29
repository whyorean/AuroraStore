/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.extensions.browse
import com.aurora.store.R
import com.aurora.store.compose.composable.LinkListItem
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.compose.ui.about.AboutDialog
import com.aurora.store.data.model.Link

@Composable
fun WelcomePage() {
    var shouldShowAboutDialog by rememberSaveable { mutableStateOf(false) }

    if (shouldShowAboutDialog) {
        AboutDialog(onDismiss = { shouldShowAboutDialog = false })
    }

    PageContent(onAboutAurora = { shouldShowAboutDialog = true })
}

@Composable
private fun PageContent(onAboutAurora: () -> Unit = {}) {
    val context = LocalContext.current

    val links = listOf(
        Link(
            id = 0,
            title = stringResource(R.string.title_about),
            subtitle = stringResource(R.string.about_aurora_store_subtitle),
            icon = R.drawable.ic_menu_about,
            url = "https://auroraoss.com/"
        ),
        Link(
            id = 1,
            title = stringResource(R.string.faqs_title),
            subtitle = stringResource(R.string.faqs_subtitle),
            icon = R.drawable.ic_faq,
            url = "https://gitlab.com/AuroraOSS/AuroraStore/-/wikis/Frequently%20Asked%20Questions"
        ),
        Link(
            id = 2,
            title = stringResource(R.string.source_code_title),
            subtitle = stringResource(R.string.source_code_subtitle),
            icon = R.drawable.ic_code,
            url = "https://gitlab.com/AuroraOSS/AuroraStore/"
        ),
        Link(
            id = 3,
            title = stringResource(R.string.menu_license),
            subtitle = stringResource(R.string.license_subtitle),
            icon = R.drawable.ic_license,
            url = "https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/LICENSE"
        ),
        Link(
            id = 4,
            title = stringResource(R.string.privacy_policy_title),
            subtitle = stringResource(R.string.privacy_policy_subtitle),
            icon = R.drawable.ic_privacy,
            url = "https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/POLICY.md"
        ),
        Link(
            id = 5,
            title = stringResource(R.string.menu_disclaimer),
            subtitle = stringResource(R.string.disclaimer_subtitle),
            icon = R.drawable.ic_disclaimer,
            url = "https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/DISCLAIMER.md"
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xxsmall))
    ) {
        stickyHeader {
            Surface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium))
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_title_welcome),
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.onboarding_welcome_select),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        items(items = links, key = { item -> item.id }) { link ->
            LinkListItem(
                link = link,
                onClick = {
                    when (link.id) {
                        0 -> onAboutAurora()
                        else -> context.browse(link.url)
                    }
                },
                iconTint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomePagePreview() {
    PreviewTemplate {
        PageContent()
    }
}
