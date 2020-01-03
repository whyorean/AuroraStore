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

import com.aurora.store.R;
import com.dragons.aurora.playstoreapiv2.Features;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;


@Data
public class App {

    private Features features;
    private ImageSource pageBackgroundImage;
    private List<String> screenshotUrls = new ArrayList<>();
    private Map<String, String> offerDetails = new HashMap<>();
    private Map<String, String> relatedLinks = new HashMap<>();
    private Rating rating = new Rating();
    private Restriction restriction;
    private Review userReview;
    private Set<String> dependencies = new HashSet<>();
    private Set<String> permissions = new HashSet<>();
    private String categoryIconUrl;
    private String categoryId;
    private String categoryName;
    private String changes;
    private String description;
    private String developerName;
    private String developerEmail;
    private String developerAddress;
    private String developerWebsite;
    private String displayName;
    private String downloadString;
    private String footerHtml;
    private String iconUrl;
    private String instantAppLink;
    private String labeledRating;
    private String packageName = "unknown";
    private String price;
    private String shortDescription;
    private String testingProgramEmail;
    private String updated;
    private String versionName = "unknown";
    private String videoUrl;
    private boolean containsAds;
    private boolean earlyAccess;
    private boolean inPlayStore;
    private boolean isAd;
    private boolean isFree;
    private boolean isInstalled;
    private boolean system;
    private boolean testingProgramAvailable;
    private boolean testingProgramOptedIn;
    private int offerType;
    private int versionCode = 0;
    private long installs;
    private long size;

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Collection<String> permissions) {
        this.permissions = new HashSet<>(permissions);
    }

    public int getInstalledVersionCode() {
        return versionCode;
    }

    public String getInstalledVersionName() {
        return versionName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof App) {
            return (((App) obj).getPackageName().equals(this.getPackageName()));
        }
        return false;
    }

    public enum Restriction {

        GENERIC(-1),
        NOT_RESTRICTED(GooglePlayAPI.AVAILABILITY_NOT_RESTRICTED),
        RESTRICTED_GEO(GooglePlayAPI.AVAILABILITY_RESTRICTED_GEO),
        INCOMPATIBLE_DEVICE(GooglePlayAPI.AVAILABILITY_INCOMPATIBLE_DEVICE_APP);

        public final int restriction;

        Restriction(int restriction) {
            this.restriction = restriction;
        }

        public static Restriction forInt(int restriction) {
            switch (restriction) {
                case GooglePlayAPI.AVAILABILITY_NOT_RESTRICTED:
                    return NOT_RESTRICTED;
                case GooglePlayAPI.AVAILABILITY_RESTRICTED_GEO:
                    return RESTRICTED_GEO;
                case GooglePlayAPI.AVAILABILITY_INCOMPATIBLE_DEVICE_APP:
                    return INCOMPATIBLE_DEVICE;
                default:
                    return GENERIC;
            }
        }

        public int getStringResId() {
            switch (restriction) {
                case GooglePlayAPI.AVAILABILITY_NOT_RESTRICTED:
                    return 0;
                case GooglePlayAPI.AVAILABILITY_RESTRICTED_GEO:
                    return R.string.availability_restriction_country;
                case GooglePlayAPI.AVAILABILITY_INCOMPATIBLE_DEVICE_APP:
                    return R.string.availability_restriction_hardware_app;
                default:
                    return R.string.availability_restriction_generic;
            }
        }
    }
}