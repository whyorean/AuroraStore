/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEachIndexed
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.store.R
import com.aurora.store.compose.preview.PreviewTemplate
import kotlinx.coroutines.launch

@Composable
fun AppsGamesScreen(
    category: StreamContract.Category,
    onNavigateToAppDetails: (packageName: String) -> Unit
) {
    ScreenContent(
        category = category,
        onNavigateToAppDetails = onNavigateToAppDetails
    )
}

@Composable
private fun ScreenContent(
    tabs: List<Int> = listOf(
        R.string.tab_for_you,
        R.string.tab_top_charts,
        R.string.tab_categories
    ),
    category: StreamContract.Category = StreamContract.Category.NONE,
    onNavigateToAppDetails: (packageName: String) -> Unit = {}
) {
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    SecondaryTabRow(
        modifier = Modifier.fillMaxWidth(),
        selectedTabIndex = pagerState.currentPage
    ) {
        tabs.fastForEachIndexed { index, title ->
            Tab(
                selected = index == pagerState.currentPage,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                text = { Text(text = stringResource(title)) }
            )
        }
    }

    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
        userScrollEnabled = false,
        verticalAlignment = Alignment.Top
    ) { page ->
        when (tabs[page]) {
            R.string.tab_for_you -> ForYouPage()
            R.string.tab_top_charts -> TopChartPage()
            R.string.tab_categories -> CategoryPage()
            else -> error("Got an unexpected page!")
        }
    }
}

@Composable
private fun ForYouPage() {

}

@Composable
private fun TopChartPage() {

}

@Composable
private fun CategoryPage() {

}

@Preview(showBackground = true)
@Composable
private fun AppsScreenPreview() {
    PreviewTemplate {
        ScreenContent(
            category = StreamContract.Category.APPLICATION
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GamesScreenPreview() {
    PreviewTemplate {
        ScreenContent(
            category = StreamContract.Category.GAME
        )
    }
}
