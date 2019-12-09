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

import com.aurora.store.iterator.ReviewStorageIterator;
import com.aurora.store.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewsHelper {

    private List<Review> reviewList = new ArrayList<>();
    private ReviewStorageIterator iterator;

    public ReviewsHelper(ReviewStorageIterator iterator) {
        this.iterator = iterator;
    }

    public void setIterator(ReviewStorageIterator iterator) {
        this.iterator = iterator;
    }

    public List<Review> getReviews() {
        reviewList.clear();
        reviewList.addAll(iterator.next());
        return reviewList;
    }
}