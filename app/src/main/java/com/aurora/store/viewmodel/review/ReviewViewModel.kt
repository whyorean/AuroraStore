/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.viewmodel.review

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.ReviewCluster
import com.aurora.gplayapi.helpers.ReviewsHelper
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class ReviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authProvider: AuthProvider
) : ViewModel() {

    var reviewsHelper: ReviewsHelper = ReviewsHelper(authProvider.authData!!)
        .using(HttpClient.getPreferredClient(context))

    val liveData: MutableLiveData<ReviewCluster> = MutableLiveData()

    private lateinit var reviewsCluster: ReviewCluster

    fun fetchReview(packageName: String, filter: Review.Filter) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    reviewsCluster = reviewsHelper.getReviews(packageName, filter)
                    liveData.postValue(reviewsCluster)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun next(nextReviewPageUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val nextReviewCluster = reviewsHelper.next(nextReviewPageUrl)
                    reviewsCluster.copy(
                        nextPageUrl = nextReviewCluster.nextPageUrl
                    ).apply {
                        reviewList.addAll(nextReviewCluster.reviewList)
                    }

                    liveData.postValue(reviewsCluster)
                } catch (_: Exception) {
                }
            }
        }
    }
}
