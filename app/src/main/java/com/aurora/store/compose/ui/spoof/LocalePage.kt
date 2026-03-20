/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.spoof

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.store.R
import com.aurora.store.compose.composable.LocaleListItem
import com.aurora.store.compose.composable.TextDividerComposable
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.viewmodel.spoof.SpoofViewModel
import java.util.Locale

@Composable
fun LocalePage(onRequestNavigateToSplash: () -> Unit, viewModel: SpoofViewModel = hiltViewModel()) {
    val availableLocales by viewModel.availableLocales.collectAsStateWithLifecycle()
    val currentLocale by viewModel.currentLocale.collectAsStateWithLifecycle()

    PageContent(
        defaultLocale = viewModel.defaultLocale,
        locales = availableLocales,
        isLocaleSelected = { locale -> currentLocale == locale },
        onLocaleSelected = { locale ->
            viewModel.onLocaleSelected(locale)
            onRequestNavigateToSplash()
        }
    )
}

@Composable
private fun PageContent(
    defaultLocale: Locale = Locale.getDefault(),
    locales: List<Locale> = emptyList(),
    isLocaleSelected: (locale: Locale) -> Boolean = { false },
    onLocaleSelected: (locale: Locale) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xxsmall))
    ) {
        stickyHeader {
            Surface(modifier = Modifier.fillMaxWidth()) {
                TextDividerComposable(
                    title = stringResource(R.string.default_spoof)
                )
            }
        }

        item {
            LocaleListItem(
                displayName = defaultLocale.displayName,
                displayLanguage = defaultLocale.getDisplayLanguage(defaultLocale),
                isChecked = isLocaleSelected(defaultLocale),
                onClick = { onLocaleSelected(defaultLocale) }
            )
        }

        stickyHeader {
            Surface(modifier = Modifier.fillMaxWidth()) {
                TextDividerComposable(
                    title = stringResource(R.string.available_spoof)
                )
            }
        }

        items(items = locales, key = { locale -> locale.hashCode() }) { locale ->
            LocaleListItem(
                displayName = locale.displayName,
                displayLanguage = locale.getDisplayLanguage(locale),
                isChecked = isLocaleSelected(locale),
                onClick = { onLocaleSelected(locale) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocalePagePreview() {
    PreviewTemplate {
        PageContent(
            locales = Locale.getAvailableLocales().toList().filter { it.displayName.isNotBlank() },
            isLocaleSelected = { locale -> locale == Locale.getDefault() }
        )
    }
}
