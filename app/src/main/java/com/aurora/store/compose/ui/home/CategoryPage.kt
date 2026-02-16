/*
 * SPDX-FileCopyrightText: 2026 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.gplayapi.data.models.Category
import com.aurora.store.R
import com.aurora.store.compose.composable.CategoryListItem
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.viewmodel.category.CategoryViewModel

@Composable
fun CategoryPage(
    type: Category.Type,
    viewModel: CategoryViewModel = hiltViewModel(
        key = type.name,
        creationCallback = { factory: CategoryViewModel.Factory ->
            factory.create(type)
        }
    )
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    ScreenContent(
        categories = categories
    )
}

@Composable
private fun ScreenContent(
    categories: List<Category> = emptyList()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xxsmall))
    ) {
        items(items = categories, key = { category -> category.title }) { category ->
            CategoryListItem(
                category = category
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryPagePreview() {
    PreviewTemplate {
        ScreenContent()
    }
}
