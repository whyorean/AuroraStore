/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
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
 */

package com.dragons.aurora;

import com.dragons.aurora.model.Review;
import com.dragons.aurora.model.ReviewBuilder;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ReviewRetrieverIterator extends ReviewIterator {

    static private final int PAGE_SIZE = 15;
    protected boolean hasNext = true;

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public List<Review> next() {
        page++;
        List<Review> list = new ArrayList<>();
        try {
            list.addAll(getReviews(packageName, PAGE_SIZE * page, PAGE_SIZE));
            if (list.size() < PAGE_SIZE) {
                hasNext = false;
            }
        } catch (IOException e) {
            // Review list does not seem important enough to let tha app crash if something happens here
            // TODO: It is unclear if this error even should be shown in the UI
            Timber.i(e.getClass().getName() + ": " + e.getMessage());
        }
        return list;
    }

    private List<Review> getReviews(String packageId, int offset, int numberOfResults) throws IOException {
        List<Review> reviews = new ArrayList<>();
        for (com.dragons.aurora.playstoreapiv2.Review review : new PlayStoreApiAuthenticator(context).getApi().reviews(
                packageId,
                GooglePlayAPI.REVIEW_SORT.HELPFUL,
                offset,
                numberOfResults
        ).getGetResponse().getReviewList()) {
            reviews.add(ReviewBuilder.build(review));
        }
        return reviews;
    }
}
