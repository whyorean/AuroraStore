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

import com.dragons.aurora.fragment.details.Review;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;

import timber.log.Timber;

public class ReviewDeleteTask extends PlayStorePayloadTask<Void> {

    private Review fragment;

    public void setFragment(Review fragment) {
        this.fragment = fragment;
    }

    @Override
    protected Void getResult(GooglePlayAPI api, String... packageNames) throws IOException {
        api.deleteReview(packageNames[0]);
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (success()) {
            fragment.clearUserReview();
        } else {
            Timber.e("Error deleting the review: %s", getException().getMessage());
        }
    }
}
