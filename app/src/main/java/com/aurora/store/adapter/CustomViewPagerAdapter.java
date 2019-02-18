package com.aurora.store.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

public class CustomViewPagerAdapter extends CustomFragmentStatePagerAdapter {
    private final List<Fragment> fragments = new ArrayList<>();

    public CustomViewPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    public void addFragment(Fragment fragment) {
        fragments.add(fragment);
    }

    public void addFragment(int index, Fragment fragment) {
        fragments.add(index, fragment);
    }


    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }
}
