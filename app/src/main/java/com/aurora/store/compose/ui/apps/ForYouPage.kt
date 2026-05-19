/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.apps

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.store.HomeStash
import com.aurora.store.compose.composable.StreamCarousel
import com.aurora.store.compose.composition.observeForced
import com.aurora.store.data.model.ViewState
import com.aurora.store.viewmodel.homestream.StreamViewModel

@Composable
internal fun ForYouContent(
    pageType: Int,
    viewModel: StreamViewModel,
    onAppClick: (App) -> Unit,
    onHeaderClick: (StreamCluster) -> Unit,
    onClusterScrolled: (StreamCluster) -> Unit,
    onScrolledToEnd: () -> Unit
) {
    val category = category(pageType)
    val state by viewModel.liveData.observeForced()

    LaunchedEffect(category) {
        viewModel.getStreamBundle(category, StreamContract.Type.HOME)
    }

    @Suppress("UNCHECKED_CAST")
    val streamBundle = (state as? ViewState.Success<*>)?.data as? HomeStash
    StreamCarousel(
        modifier = Modifier.fillMaxSize(),
        streamBundle = streamBundle?.get(category),
        onHeaderClick = onHeaderClick,
        onAppClick = onAppClick,
        onClusterScrolled = onClusterScrolled,
        onScrolledToEnd = onScrolledToEnd
    )
}
