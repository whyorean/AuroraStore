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

package com.dragons.aurora.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.dragons.aurora.R;
import com.dragons.aurora.fragment.CategoryListFragment;
import com.dragons.aurora.fragment.HomeFragment;
import com.dragons.aurora.fragment.InstalledAppsFragment;
import com.dragons.aurora.fragment.SearchFragment;
import com.dragons.aurora.fragment.UpdatableAppsFragment;

public class ViewPagerAdapter extends FragmentPagerAdapter {
    private Context mContext;

    public ViewPagerAdapter(Context context, FragmentManager fragmentManager) {
        super(fragmentManager);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new InstalledAppsFragment();
            case 2:
                return new UpdatableAppsFragment();
            case 3:
                return new CategoryListFragment();
            case 4:
                return new SearchFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 5;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return mContext.getString(R.string.action_home);
            case 1:
                return mContext.getString(R.string.action_myApps);
            case 2:
                return mContext.getString(R.string.action_updates);
            case 3:
                return mContext.getString(R.string.action_categories);
            case 4:
                return mContext.getString(R.string.search_title);
            default:
                return null;
        }
    }

}
