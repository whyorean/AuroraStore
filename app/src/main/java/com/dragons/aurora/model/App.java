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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class App implements Comparable<App> {

    private PackageInfo packageInfo;

    private String displayName;
    private String versionName;
    private int versionCode;
    private int offerType;
    private String updated;
    private long size;
    private int installs;
    private Rating rating = new Rating();
    private String categoryIconUrl;
    private ImageSource pageBackgroundImage;
    private String iconUrl;
    private String videoUrl;
    private String ratingURL;
    private String changes;
    private String developerName;
    private String description;
    private Set<String> permissions = new HashSet<>();
    private boolean isInstalled;
    private boolean isFree;
    private List<String> screenshotUrls = new ArrayList<>();
    private Review userReview;
    private String categoryId;
    private String price;
    private boolean containsAds;
    private Set<String> dependencies = new HashSet<>();
    private Map<String, String> offerDetails = new HashMap<>();
    private boolean system;
    private boolean inPlayStore;
    private Map<String, String> relatedLinks = new HashMap<>();
    private boolean earlyAccess;
    private boolean testingProgramAvailable;
    private boolean testingProgramOptedIn;
    private String testingProgramEmail;
    private int restriction;

    private String labeledRating;

    public App() {
        this.packageInfo = new PackageInfo();
    }

    public App(PackageInfo packageInfo) {
        this.setPackageInfo(packageInfo);
        this.setVersionName(packageInfo.versionName);
        this.setVersionCode(packageInfo.versionCode);
        this.setInstalled(true);
        this.setSystem(null != packageInfo.applicationInfo && (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
        if (null != packageInfo.requestedPermissions) {
            this.setPermissions(Arrays.asList(packageInfo.requestedPermissions));
        }
    }

    public String getLabeledRating() {
        return labeledRating;
    }

    public void setLabeledRating(String labeledRating) {
        this.labeledRating = labeledRating;
    }

    public String getRatingURL() {
        return ratingURL;
    }

    public void setRatingURL(String ratingURL) {
        this.ratingURL = ratingURL;
    }

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    public void setPackageInfo(PackageInfo packageInfo) {
        this.packageInfo = packageInfo;
    }

    public String getPackageName() {
        return packageInfo.packageName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public int getOfferType() {
        return offerType;
    }

    public void setOfferType(int offerType) {
        this.offerType = offerType;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getInstalls() {
        return installs;
    }

    public void setInstalls(int installs) {
        this.installs = installs;
    }

    public Rating getRating() {
        return rating;
    }

    public String getCategoryIconUrl() {
        return categoryIconUrl;
    }

    public void setCategoryIconUrl(String categoryIconUrl) {
        this.categoryIconUrl = categoryIconUrl;
    }

    public ImageSource getPageBackgroundImage() {
        return pageBackgroundImage;
    }

    public void setPageBackgroundImage(ImageSource pageBackgroundImage) {
        this.pageBackgroundImage = pageBackgroundImage;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public ImageSource getIconInfo() {
        ImageSource imageSource = new ImageSource();
        if (null != packageInfo && null != packageInfo.applicationInfo) {
            imageSource.setApplicationInfo(packageInfo.applicationInfo);
        }
        if (!TextUtils.isEmpty(iconUrl)) {
            imageSource.setUrl(iconUrl);
        }
        return imageSource;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

    public String getDeveloperName() {
        return developerName;
    }

    public void setDeveloperName(String developerName) {
        this.developerName = developerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Collection<String> permissions) {
        this.permissions = new HashSet<>(permissions);
    }

    public boolean isInstalled() {
        return isInstalled;
    }

    public void setInstalled(boolean installed) {
        isInstalled = installed;
    }

    public int getInstalledVersionCode() {
        if (null != packageInfo) {
            return packageInfo.versionCode;
        }
        return 0;
    }

    public boolean isFree() {
        return isFree;
    }

    public void setFree(boolean free) {
        isFree = free;
    }

    public List<String> getScreenshotUrls() {
        return screenshotUrls;
    }

    public Review getUserReview() {
        return userReview;
    }

    public void setUserReview(Review userReview) {
        this.userReview = userReview;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public boolean containsAds() {
        return containsAds;
    }

    public void setContainsAds(boolean containsAds) {
        this.containsAds = containsAds;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public Map<String, String> getOfferDetails() {
        return offerDetails;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public boolean isInPlayStore() {
        return inPlayStore;
    }

    public void setInPlayStore(boolean inPlayStore) {
        this.inPlayStore = inPlayStore;
    }

    public Map<String, String> getRelatedLinks() {
        return relatedLinks;
    }

    public boolean isEarlyAccess() {
        return earlyAccess;
    }

    public void setEarlyAccess(boolean earlyAccess) {
        this.earlyAccess = earlyAccess;
    }

    public boolean isTestingProgramAvailable() {
        return testingProgramAvailable;
    }

    public void setTestingProgramAvailable(boolean testingProgramAvailable) {
        this.testingProgramAvailable = testingProgramAvailable;
    }

    public boolean isTestingProgramOptedIn() {
        return testingProgramOptedIn;
    }

    public void setTestingProgramOptedIn(boolean testingProgramOptedIn) {
        this.testingProgramOptedIn = testingProgramOptedIn;
    }

    public String getTestingProgramEmail() {
        return testingProgramEmail;
    }

    public void setTestingProgramEmail(String testingProgramEmail) {
        this.testingProgramEmail = testingProgramEmail;
    }

    public int getRestriction() {
        return restriction;
    }

    public void setRestriction(int restriction) {
        this.restriction = restriction;
    }

    @Override
    public int compareTo(App o) {
        return getDisplayName().compareToIgnoreCase(o.getDisplayName());
    }
}
