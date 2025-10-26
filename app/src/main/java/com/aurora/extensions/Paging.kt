/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import androidx.compose.runtime.Composable
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.flowOf

/**
 * Empty lazy paging item flow for optional methods
 */
@Composable
fun <T: Any> emptyPagingItems(): LazyPagingItems<T> {
    return flowOf(PagingData.empty<T>()).collectAsLazyPagingItems()
}
