package com.aurora.store.adapter;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.aurora.store.R;
import com.aurora.store.fragment.TopFreeApps;
import com.aurora.store.fragment.TopGrossingApps;
import com.aurora.store.fragment.TopTrendingApps;

import org.jetbrains.annotations.NotNull;

public class SubCategoryAdapter extends FragmentStatePagerAdapter {
    private Context mContext;

    public SubCategoryAdapter(Context context, FragmentManager fragmentManager) {
        super(fragmentManager);
        mContext = context;
    }

    @NotNull
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return new TopFreeApps();
        } else if (position == 1) {
            return new TopGrossingApps();
        } else {
            return new TopTrendingApps();
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return mContext.getString(R.string.category_topFree);
            case 1:
                return mContext.getString(R.string.category_trending);
            case 2:
                return mContext.getString(R.string.category_topGrossing);
            default:
                return null;
        }
    }

}