/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
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

package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.model.Review;
import com.aurora.store.model.ReviewBuilder;
import com.dragons.aurora.playstoreapiv2.ReviewResponse;

import java.io.IOException;

public class ReviewAdder extends BaseTask {

    public ReviewAdder(Context context) {
        super(context);
    }

    public boolean submit(String packageName, Review review) throws IOException {
        ReviewResponse response = getApi().addOrEditReview(
                packageName,
                review.getComment(),
                review.getTitle(),
                review.getRating());
        Review apiReview = ReviewBuilder.build(response.getUserReview());
        return apiReview != null;
    }
}
