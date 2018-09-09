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

package com.dragons.aurora.task.playstore;

import com.dragons.aurora.ReviewStorageIterator;
import com.dragons.aurora.model.Review;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;
import java.util.List;

import timber.log.Timber;

public class ReviewLoadTask extends PlayStorePayloadTask<List<Review>> {

    private ReviewStorageIterator iterator;
    private com.dragons.aurora.fragment.details.Review fragment;
    private boolean next;

    public void setIterator(ReviewStorageIterator iterator) {
        this.iterator = iterator;
    }

    public void setFragment(com.dragons.aurora.fragment.details.Review fragment) {
        this.fragment = fragment;
    }

    public void setNext(boolean next) {
        this.next = next;
    }

    @Override
    protected List<Review> getResult(GooglePlayAPI api, String... arguments) throws IOException {
        return next ? iterator.next() : iterator.previous();
    }

    @Override
    protected void onPostExecute(List<Review> reviews) {
        super.onPostExecute(reviews);
        if (success() && fragment != null) {
            fragment.showReviews(reviews);
        } else {
            Timber.e("Could not get reviews: %s", getException().getMessage());
        }
    }
}