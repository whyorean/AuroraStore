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
import com.aurora.extensions.close
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.ReviewCluster
import com.aurora.store.R
import com.aurora.store.databinding.ActivityDetailsReviewBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.details.ReviewViewModel_
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.viewmodel.review.ReviewViewModel

class DetailsReviewActivity : BaseActivity() {

    private lateinit var B: ActivityDetailsReviewBinding
    private lateinit var VM: ReviewViewModel

    private lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener

    private lateinit var app: App
    private lateinit var filter: Review.Filter
    private lateinit var reviewCluster: ReviewCluster

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
            if (!::reviewCluster.isInitialized) {
                attachRecycler()
            }

            it?.let {
                reviewCluster = it
                updateController(reviewCluster)
            }
        })

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
        B.chipGroup.setOnCheckedChangeListener { _, checkedId ->
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
                if (::reviewCluster.isInitialized) {
                    VM.next(reviewCluster.nextPageUrl)
                }
            }
        }
        B.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
    }

    private fun updateController(reviewCluster: ReviewCluster) {
        B.recycler.withModels {
            setFilterDuplicates(true)
            reviewCluster.reviewList.forEach {
                add(
                    ReviewViewModel_()
                        .id(it.commentId)
                        .review(it)
                )
            }

            if (reviewCluster.hasNext()){
                add(
                    AppProgressViewModel_()
                        .id("progress")
                )
            }
        }
    }

    private fun fetchReviews() {
        VM.fetchReview(app.packageName, filter)
    }

    private fun resetPage() {
        endlessRecyclerOnScrollListener.resetPageCount()
    }
}
