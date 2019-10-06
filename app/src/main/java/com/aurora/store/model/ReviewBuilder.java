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

package com.aurora.store.model;

import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.Image;

public class ReviewBuilder {

    public static Review build(com.dragons.aurora.playstoreapiv2.Review reviewProto) {
        final Review review = new Review();
        review.setComment(reviewProto.getComment());
        review.setTitle(reviewProto.getTitle());
        review.setRating(reviewProto.getStarRating());
        review.setUserName(reviewProto.getUserProfile().getName());
        review.setTimeStamp(reviewProto.getTimestampMsec());
        for (Image image : reviewProto.getUserProfile().getImageList()) {
            if (image.getImageType() == GooglePlayAPI.IMAGE_TYPE_APP_ICON) {
                review.setUserPhotoUrl(image.getImageUrl());
            }
        }
        return review;
    }
}
