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

import com.dragons.aurora.Aurora;

public class Filter {

    private boolean systemApps;
    private boolean appsWithAds;
    private boolean paidApps;
    private boolean gsfDependentApps;
    private String category = Aurora.TOP;
    private float rating;
    private int downloads;

    public boolean isSystemApps() {
        return systemApps;
    }

    public void setSystemApps(boolean systemApps) {
        this.systemApps = systemApps;
    }

    public boolean isAppsWithAds() {
        return appsWithAds;
    }

    public void setAppsWithAds(boolean appsWithAds) {
        this.appsWithAds = appsWithAds;
    }

    public boolean isPaidApps() {
        return paidApps;
    }

    public void setPaidApps(boolean paidApps) {
        this.paidApps = paidApps;
    }

    public boolean isGsfDependentApps() {
        return gsfDependentApps;
    }

    public void setGsfDependentApps(boolean gsfDependentApps) {
        this.gsfDependentApps = gsfDependentApps;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getDownloads() {
        return downloads;
    }

    public void setDownloads(int downloads) {
        this.downloads = downloads;
    }
}
