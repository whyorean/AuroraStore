/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aurora.store.R
import com.aurora.store.compose.composable.PageIndicator
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.compose.ui.onboarding.navigation.ExtraScreen
import com.aurora.store.viewmodel.onboarding.OnboardingViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val pages = listOfNotNull(
        ExtraScreen.Welcome,
        ExtraScreen.Permissions
    )

    ScreenContent(
        pages = pages,
        onFinishOnboarding = { viewModel.finishOnboarding() }
    )
}

@Composable
private fun ScreenContent(
    pages: List<ExtraScreen> = emptyList(),
    onFinishOnboarding: () -> Unit = {}
) {
    val pagerState = rememberPagerState { pages.size }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    PageIndicator(totalPages = pages.size, currentPage = pagerState.currentPage)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HorizontalPager(
                modifier = Modifier.weight(1F),
                state = pagerState,
                verticalAlignment = Alignment.Top
            ) { page ->
                when (pages[page]) {
                    ExtraScreen.Welcome -> WelcomeScreen()
                    ExtraScreen.Permissions -> PermissionsScreen()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.padding_medium)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
            ) {
                TextButton(
                    modifier = Modifier.weight(1F),
                    enabled = pagerState.currentPage != 0,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_back),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    modifier = Modifier.weight(1F),
                    onClick = {
                        when {
                            pagerState.currentPage < (pagerState.pageCount - 1) -> {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }

                            else -> onFinishOnboarding()
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_next),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    PreviewTemplate {
        ScreenContent(
            pages = listOf(ExtraScreen.Welcome, ExtraScreen.Permissions)
        )
    }
}
