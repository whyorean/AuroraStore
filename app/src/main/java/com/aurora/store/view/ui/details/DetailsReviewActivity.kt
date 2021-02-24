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

package com.aurora.store.view.ui.details

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Review
import com.aurora.store.R
import com.aurora.store.data.ViewState
import com.aurora.store.databinding.ActivityDetailsReviewBinding
import com.aurora.extensions.close
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.details.ReviewViewModel_
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.viewmodel.review.ReviewViewModel

class DetailsReviewActivity : BaseActivity() {

    private lateinit var B: ActivityDetailsReviewBinding
    private lateinit var VM: ReviewViewModel
    private lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener
    private lateinit var app: App
    private lateinit var filter: Review.Filter

    override fun onConnected() {

    }

    override fun onDisconnected() {

    }

    override fun onReconnected() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        B = ActivityDetailsReviewBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this).get(ReviewViewModel::class.java)

        setContentView(B.root)

        VM.liveData.observe(this, {
            when (it) {
                is ViewState.Empty -> {
                }
                is ViewState.Loading -> {
                }
                is ViewState.Error -> {
                }
                is ViewState.Success<*> -> {
                    updateController(it.data as List<Review>)
                }
                else -> {

                }
            }
        })

        attachRecycler()
        attachChips()

        val itemRaw: String? = intent.getStringExtra(Constants.STRING_EXTRA)
        if (itemRaw != null) {
            app = gson.fromJson(itemRaw, App::class.java)
            filter = Review.Filter.ALL

            app.let {
                attachToolbar()
                fetchReviews()
            }
        }
    }

    private fun attachToolbar() {
        B.layoutToolbarActionReview.toolbar.setOnClickListener {
            close()
        }
        B.layoutToolbarActionReview.txtTitle.text = app.displayName
    }

    private fun attachChips() {
        B.chipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.filter_review_all -> filter = Review.Filter.ALL
                R.id.filter_review_critical -> filter = Review.Filter.CRITICAL
                R.id.filter_review_positive -> filter = Review.Filter.POSITIVE
                R.id.filter_review_five -> filter = Review.Filter.FIVE
                R.id.filter_review_four -> filter = Review.Filter.FOUR
                R.id.filter_review_three -> filter = Review.Filter.THREE
                R.id.filter_review_two -> filter = Review.Filter.TWO
                R.id.filter_review_one -> filter = Review.Filter.ONE
            }

            resetPage()
            fetchReviews()
        }
    }

    private fun attachRecycler() {
        endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                VM.fetchReview(app.packageName, filter)
            }
        }
        B.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
    }

    private fun updateController(reviews: List<Review>) {
        B.recycler.withModels {
            reviews.forEach {
                add(
                    ReviewViewModel_()
                        .id(it.userName)
                        .review(it)
                )
            }
        }
    }

    private fun fetchReviews() {
        VM.fetchReview(app.packageName, filter)
    }

    private fun resetPage() {
        endlessRecyclerOnScrollListener.resetPageCount()
        B.recycler.clear()
        VM.reset()
    }
}
