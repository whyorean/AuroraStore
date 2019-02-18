package com.aurora.store.model;

import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.Image;

public class ReviewBuilder {

    public static Review build(com.dragons.aurora.playstoreapiv2.Review reviewProto) {
        Review review = new Review();
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
