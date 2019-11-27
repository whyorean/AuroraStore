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

import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.PrefUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.Image;
import com.dragons.aurora.playstoreapiv2.UserProfile;

public class UserProfiler extends BaseTask {
    public UserProfiler(Context context) {
        super(context);
    }

    public boolean getUserProfile() throws Exception {
        GooglePlayAPI api = getApi();
        UserProfile userProfile = api.userProfile().getUserProfile();
        if (userProfile == null)
            return false;
        else {
            PrefUtil.putString(context, Accountant.GOOGLE_NAME, userProfile.getName());
            for (Image image : userProfile.getImageList()) {
                if (image.getImageType() == GooglePlayAPI.IMAGE_TYPE_APP_ICON) {
                    PrefUtil.putString(context, Accountant.GOOGLE_URL, image.getImageUrl());
                }
            }
            return true;
        }
    }
}
