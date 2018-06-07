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

package com.dragons.aurora.model;

import android.text.TextUtils;

import com.dragons.aurora.Util;
import com.dragons.aurora.playstoreapiv2.AggregateRating;
import com.dragons.aurora.playstoreapiv2.AppDetails;
import com.dragons.aurora.playstoreapiv2.Badge;
import com.dragons.aurora.playstoreapiv2.Dependency;
import com.dragons.aurora.playstoreapiv2.DetailsResponse;
import com.dragons.aurora.playstoreapiv2.DocV2;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.Image;
import com.dragons.aurora.playstoreapiv2.RelatedLink;
import com.dragons.aurora.playstoreapiv2.Unknown25Item;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppBuilder {

    static public App build(DetailsResponse detailsResponse) {
        App app = build(detailsResponse.getDocV2());
        if (TextUtils.isEmpty(app.getCategoryIconUrl()) || app.getRelatedLinks().isEmpty()) {
            walkBadges(app, detailsResponse.getBadgeList());
        }
        if (detailsResponse.hasUserReview()) {
            app.setUserReview(ReviewBuilder.build(detailsResponse.getUserReview()));
        }
        return app;
    }

    static public App build(DocV2 details) {
        App app = new App();
        app.setDisplayName(details.getTitle());
        app.setDescription(details.getDescriptionHtml());
        app.setCategoryId(details.getRelatedLinks().getCategoryInfo().getAppCategory());
        app.setRestriction(details.getAvailability().getRestriction());
        if (details.getOfferCount() > 0) {
            app.setOfferType(details.getOffer(0).getOfferType());
            app.setFree(details.getOffer(0).getMicros() == 0);
            app.setPrice(details.getOffer(0).getFormattedAmount());
        }
        fillOfferDetails(app, details);
        fillAggregateRating(app, details.getAggregateRating());
        fillRelatedLinks(app, details);
        AppDetails appDetails = details.getDetails().getAppDetails();
        app.getPackageInfo().packageName = appDetails.getPackageName();
        app.setVersionName(appDetails.getVersionString());
        app.setVersionCode(appDetails.getVersionCode());
        app.setDeveloperName(appDetails.getDeveloperName());
        app.setSize(appDetails.getInstallationSize());
        app.setInstalls(getInstallsNum(appDetails.getNumDownloads()));
        app.setUpdated(appDetails.getUploadDate());
        app.setChanges(appDetails.getRecentChangesHtml());
        app.setPermissions(appDetails.getPermissionList());
        app.setContainsAds(appDetails.hasContainsAds() && !TextUtils.isEmpty(appDetails.getContainsAds()));
        app.setInPlayStore(true);
        app.setEarlyAccess(appDetails.hasEarlyAccessInfo());
        app.setTestingProgramAvailable(appDetails.hasTestingProgramInfo());
        if (app.isTestingProgramAvailable()) {
            app.setTestingProgramOptedIn(appDetails.getTestingProgramInfo().hasSubscribed() && appDetails.getTestingProgramInfo().getSubscribed());
            app.setTestingProgramEmail(appDetails.getTestingProgramInfo().getTestingProgramEmail());
        }
        fillImages(app, details.getImageList());
        fillDependencies(app, appDetails);
        app.setLabeledRating(details.getRelatedLinks().getRated().getLabel());
        app.setRatingURL(details.getRelatedLinks().getRated().getImage().getImageUrl());
        return app;
    }

    static private int getInstallsNum(String installsRaw) {
        Matcher matcher = Pattern.compile("[\\d]+").matcher(installsRaw.replaceAll("[,\\.\\s]+", ""));
        if (matcher.find()) {
            return Util.parseInt(matcher.group(0), 0);
        }
        return 0;
    }

    static private void fillAggregateRating(App app, AggregateRating aggregateRating) {
        Rating rating = app.getRating();
        rating.setAverage(aggregateRating.getStarRating());
        rating.setStars(1, (int) aggregateRating.getOneStarRatings());
        rating.setStars(2, (int) aggregateRating.getTwoStarRatings());
        rating.setStars(3, (int) aggregateRating.getThreeStarRatings());
        rating.setStars(4, (int) aggregateRating.getFourStarRatings());
        rating.setStars(5, (int) aggregateRating.getFiveStarRatings());
    }

    static private void fillDependencies(App app, AppDetails appDetails) {
        if (!appDetails.hasDependencies() || appDetails.getDependencies().getDependencyCount() == 0) {
            return;
        }
        for (Dependency dep : appDetails.getDependencies().getDependencyList()) {
            app.getDependencies().add(dep.getPackageName());
        }
    }

    static private void fillOfferDetails(App app, DocV2 details) {
        if (!details.hasUnknown25() || details.getUnknown25().getItemCount() == 0) {
            return;
        }
        for (Unknown25Item item : details.getUnknown25().getItemList()) {
            if (!item.hasContainer()) {
                continue;
            }
            app.getOfferDetails().put(item.getLabel(), item.getContainer().getValue());
        }
    }

    static private void fillRelatedLinks(App app, DocV2 details) {
        if (!details.hasRelatedLinks()) {
            return;
        }
        for (RelatedLink link : details.getRelatedLinks().getRelatedLinksList()) {
            if (!link.hasLabel() || !link.hasUrl1()) {
                continue;
            }
            app.getRelatedLinks().put(link.getLabel(), link.getUrl1());
        }
    }

    static private void fillImages(App app, List<Image> images) {
        for (Image image : images) {
            switch (image.getImageType()) {
                case GooglePlayAPI.IMAGE_TYPE_CATEGORY_ICON:
                    app.setCategoryIconUrl(image.getImageUrl());
                    break;
                case GooglePlayAPI.IMAGE_TYPE_APP_ICON:
                    app.setIconUrl(image.getImageUrl());
                    break;
                case GooglePlayAPI.IMAGE_TYPE_YOUTUBE_VIDEO_LINK:
                    app.setVideoUrl(image.getImageUrl());
                    break;
                case GooglePlayAPI.IMAGE_TYPE_PLAY_STORE_PAGE_BACKGROUND:
                    app.setPageBackgroundImage(new ImageSource(image.getImageUrl()).setFullSize(true));
                    break;
                case GooglePlayAPI.IMAGE_TYPE_APP_SCREENSHOT:
                    app.getScreenshotUrls().add(image.getImageUrl());
                    break;
            }
        }
    }

    static private void walkBadges(App app, List<Badge> badges) {
        for (Badge badge : badges) {
            String link = getLink(badge);
            if (TextUtils.isEmpty(link)) {
                continue;
            }
            if (app.getRelatedLinks().isEmpty() && link.startsWith("browse")) {
                // That's similar apps
                app.getRelatedLinks().put(badge.getLabel(), link);
            } else if (link.startsWith("homeV2?cat=")) {
                // That's category badge
                app.setCategoryIconUrl(badge.getImage().getImageUrl());
            }
        }
    }

    static private String getLink(Badge badge) {
        if (null != badge
                && badge.hasBadgeContainer1()
                && badge.getBadgeContainer1().hasBadgeContainer2()
                && badge.getBadgeContainer1().getBadgeContainer2().hasBadgeLinkContainer()
                ) {
            return badge.getBadgeContainer1().getBadgeContainer2().getBadgeLinkContainer().getLink();
        }
        return null;
    }
}
