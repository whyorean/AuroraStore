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

package com.aurora.store.adapter;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.aurora.store.R;
import com.aurora.store.fragment.SubCategoryFragment;

import org.jetbrains.annotations.NotNull;

public class SubCategoryAdapter extends FragmentStatePagerAdapter {

    private Context context;

    public SubCategoryAdapter(Context context, FragmentManager fragmentManager) {
        super(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.context = context;
    }

    @NotNull
    @Override
    public Fragment getItem(int position) {
        final Bundle bundle = new Bundle();
        final Fragment subCategoryFragment = new SubCategoryFragment();
        switch (position) {
            case 0:
                bundle.putString("SUBCATEGORY", "TOP_FREE");
                break;
            case 1:
                bundle.putString("SUBCATEGORY", "MOVERS_SHAKERS");
                break;
            default:
                bundle.putString("SUBCATEGORY", "TOP_GROSSING");
                break;
        }
        subCategoryFragment.setArguments(bundle);
        return subCategoryFragment;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getString(R.string.category_topFree);
            case 1:
                return context.getString(R.string.category_trending);
            default:
                return context.getString(R.string.category_topGrossing);
        }
    }
}