/*
 * SPDX-FileCopyrightText: 2021-2023 AuroraOSS
 * SPDX-FileCopyrightText: 2024-2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.review

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.helpers.ReviewsHelper
import com.aurora.store.data.paging.GenericPagingSource.Companion.createPager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel(assistedFactory = DetailsReviewViewModel.Factory::class)
class DetailsReviewViewModel @AssistedInject constructor(
    @Assisted private val packageName: String,
    private val reviewsHelper: ReviewsHelper
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(packageName: String): DetailsReviewViewModel
    }

    private val TAG = DetailsReviewViewModel::class.java.simpleName

    private var reviewsNextPageUrl: String? = null
    private val _reviews = MutableStateFlow<PagingData<Review>>(PagingData.empty())
    val reviews = _reviews.asStateFlow()

    init {
        fetchReviews()
    }

    fun fetchReviews(filter: Review.Filter = Review.Filter.ALL) {
        reviewsNextPageUrl = null

        createPager { page ->
            try {
                when (page) {
                    1 -> reviewsHelper.getReviews(packageName, filter).also {
                        reviewsNextPageUrl = it.nextPageUrl
                    }.reviewList

                    else -> {
                        if (!reviewsNextPageUrl.isNullOrBlank()) {
                            reviewsHelper.next(reviewsNextPageUrl!!).also {
                                reviewsNextPageUrl = it.nextPageUrl
                            }.reviewList
                        } else {
                            emptyList()
                        }
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch reviews for $page: $reviewsNextPageUrl", exception)
                emptyList()
            }
        }.flow.distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _reviews.value = it }
            .launchIn(viewModelScope)
    }
}
