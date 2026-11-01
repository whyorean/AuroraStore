/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.extensions.TAG
import com.aurora.gplayapi.PlayClient
import com.aurora.gplayapi.PlayException
import com.aurora.gplayapi.model.Review
import com.aurora.store.AuroraApp
import com.aurora.store.data.PageResult
import com.aurora.store.data.event.AuthEvent
import com.aurora.store.data.paging.GenericPagingSource.Companion.manualPager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel(assistedFactory = ReviewViewModel.Factory::class)
class ReviewViewModel @AssistedInject constructor(
    @Assisted private val packageName: String,
    private val playClient: PlayClient
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(packageName: String): ReviewViewModel
    }

    private val _reviews = MutableStateFlow<PagingData<Review>>(PagingData.Companion.empty())
    val reviews = _reviews.asStateFlow()

    init {
        fetchReviews()
    }

    fun fetchReviews(filter: Review.Filter = Review.Filter.ALL) {
        var reviewsNextPageUrl: String? = null

        manualPager { page ->
            val items = try {
                when (page) {
                    1 -> playClient.reviews.reviews(packageName, filter).also {
                        reviewsNextPageUrl = it.nextPageUrl
                    }.reviews

                    else -> {
                        if (!reviewsNextPageUrl.isNullOrBlank()) {
                            playClient.reviews.next(reviewsNextPageUrl!!).also {
                                reviewsNextPageUrl = it.nextPageUrl
                            }.reviews
                        } else {
                            emptyList()
                        }
                    }
                }
            } catch (exception: PlayException.Unauthorized) {
                Log.w(TAG, "Reviews fetch returned ${exception.code}, redirecting to Splash")
                AuroraApp.events.send(AuthEvent.SessionExpired(packageName))
                emptyList()
            }
            PageResult(items)
        }.flow.distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _reviews.value = it }
            .launchIn(viewModelScope)
    }
}
