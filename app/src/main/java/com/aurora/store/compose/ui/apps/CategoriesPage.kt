/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.apps

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.aurora.gplayapi.data.models.Category
import com.aurora.store.CategoryStash
import com.aurora.store.R
import com.aurora.store.compose.composable.CategoryItem
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.rememberShimmerBrush
import com.aurora.store.compose.composable.ShimmerCategoryRow
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.data.model.ViewState
import com.aurora.store.viewmodel.category.CategoryViewModel

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun CategoriesContent(
    pageType: Int,
    viewModel: CategoryViewModel,
    onCategoryClick: (Category) -> Unit,
    bottomContentPadding: Dp
) {
    val categoryType = if (pageType == 1) Category.Type.GAME else Category.Type.APPLICATION
    val state by viewModel.liveData.observeAsState()

    @Suppress("UNCHECKED_CAST")
    val categories = (state as? ViewState.Success<*>)?.data as? CategoryStash
    val list = categories?.get(categoryType)

    LaunchedEffect(categoryType) {
        viewModel.getCategoryList(categoryType)
    }

    if (state is ViewState.Error) {
        Placeholder(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.ic_refresh),
            message = stringResource(R.string.error),
            actionLabel = stringResource(R.string.action_retry),
            onAction = { viewModel.getCategoryList(categoryType) }
        )
        return
    }

    CategoriesBody(
        list = list,
        onCategoryClick = onCategoryClick,
        bottomContentPadding = bottomContentPadding
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoriesBody(
    list: List<Category>?,
    onCategoryClick: (Category) -> Unit = {},
    bottomContentPadding: Dp = 0.dp
) {
    if (list.isNullOrEmpty()) {
        val shimmerBrush = rememberShimmerBrush()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = dimensionResource(R.dimen.spacing_large),
                end = dimensionResource(R.dimen.spacing_large),
                top = 12.dp,
                bottom = bottomContentPadding
            ),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                ListItemDefaults.SegmentedGap
            )
        ) {
            items(10) { ShimmerCategoryRow(shimmerBrush) }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = dimensionResource(R.dimen.spacing_large),
                end = dimensionResource(R.dimen.spacing_large),
                top = 12.dp,
                bottom = bottomContentPadding
            ),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                ListItemDefaults.SegmentedGap
            )
        ) {
            items(count = list.size, key = { list[it].title }) { index ->
                CategoryItem(
                    category = list[index],
                    itemIndex = index,
                    itemCount = list.size,
                    onClick = { onCategoryClick(list[index]) }
                )
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun CategoriesBodyLoadingPreview() {
    CategoriesBody(list = null)
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun CategoriesBodyLoadedPreview() {
    val categories = listOf(
        "Art & Design",
        "Auto & Vehicles",
        "Beauty",
        "Books & Reference",
        "Business",
        "Comics",
        "Communication",
        "Dating",
        "Education",
        "Entertainment"
    ).map { Category(title = it, imageUrl = "") }
    CategoriesBody(list = categories)
}
