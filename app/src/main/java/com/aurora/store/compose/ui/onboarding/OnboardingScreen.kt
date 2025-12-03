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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aurora.extensions.isWindowCompact
import com.aurora.store.R
import com.aurora.store.compose.composable.PageIndicator
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.compose.ui.onboarding.navigation.OnboardingPage
import com.aurora.store.viewmodel.onboarding.OnboardingViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val pages = listOfNotNull(
        OnboardingPage.WELCOME,
        OnboardingPage.PERMISSIONS
    )

    ScreenContent(
        pages = pages,
        onFinishOnboarding = { viewModel.finishOnboarding() }
    )
}

@Composable
private fun ScreenContent(
    pages: List<OnboardingPage> = emptyList(),
    onFinishOnboarding: () -> Unit = {}
) {
    val pagerState = rememberPagerState { pages.size }
    val coroutineScope = rememberCoroutineScope()

    fun isFinalPage(): Boolean {
        return pagerState.currentPage == (pagerState.pageCount - 1)
    }

    @Composable
    fun SetupActions(windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()) {
        val horizontalButtonPadding = when {
            windowAdaptiveInfo.isWindowCompact -> dimensionResource(R.dimen.padding_medium)
            else -> dimensionResource(R.dimen.padding_xlarge)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = dimensionResource(R.dimen.padding_medium),
                    horizontal = horizontalButtonPadding
                ),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
        ) {
            val buttonWidthModifier = when {
                windowAdaptiveInfo.isWindowCompact -> Modifier.weight(1F)
                else -> Modifier.widthIn(min = dimensionResource(R.dimen.width_button))
            }

            TextButton(
                modifier = buttonWidthModifier,
                onClick = {
                    when (pagerState.currentPage) {
                        0 -> onFinishOnboarding()
                        else -> {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    }
                }
            ) {
                Text(
                    text = when (pagerState.currentPage) {
                        0 -> stringResource(R.string.action_skip)
                        else -> stringResource(R.string.action_back)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                modifier = buttonWidthModifier,
                onClick = {
                    when {
                        isFinalPage() -> onFinishOnboarding()
                        else -> {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                }
            ) {
                Text(
                    text = when {
                        isFinalPage() -> stringResource(R.string.action_finish)
                        else -> stringResource(R.string.action_next)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

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
                    OnboardingPage.WELCOME -> WelcomePage()
                    OnboardingPage.PERMISSIONS -> PermissionsPage()
                }
            }

            SetupActions()
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    PreviewTemplate {
        ScreenContent(
            pages = listOf(OnboardingPage.WELCOME, OnboardingPage.PERMISSIONS)
        )
    }
}
