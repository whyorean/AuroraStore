/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.aurora.store.iterator;

import com.aurora.store.AuroraApplication;
import com.aurora.store.exception.InvalidApiException;
import com.aurora.store.model.Review;
import com.aurora.store.model.ReviewBuilder;
import com.aurora.store.util.Log;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.util.ArrayList;
import java.util.List;

public class ReviewRetrieverIterator extends ReviewIterator {

    static private final int PAGE_SIZE = 15;
    protected boolean hasNext = true;

    private GooglePlayAPI.REVIEW_SORT reviewSort = GooglePlayAPI.REVIEW_SORT.HIGHRATING;

    public void setReviewSort(GooglePlayAPI.REVIEW_SORT reviewSort) {
        this.reviewSort = reviewSort;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public List<Review> next() {
        page++;
        List<Review> list = new ArrayList<>();
        try {
            list.addAll(getReviews(packageName, PAGE_SIZE * page, reviewSort));
            if (list.size() < PAGE_SIZE) {
                hasNext = false;
            }
        } catch (Exception e) {
            Log.i(e.getClass().getName() + ": " + e.getMessage());
        }
        return list;
    }

    private List<Review> getReviews(String packageId, int offset, GooglePlayAPI.REVIEW_SORT reviewSort) throws Exception {
        List<Review> reviews = new ArrayList<>();
        GooglePlayAPI api = AuroraApplication.api;
        if (api == null)
            throw new InvalidApiException();
        for (com.dragons.aurora.playstoreapiv2.Review review : api.reviews(
                packageId,
                reviewSort,
                offset,
                PAGE_SIZE
        ).getGetResponse().getReviewList()) {
            reviews.add(ReviewBuilder.build(review));
        }
        return reviews;
    }
}
