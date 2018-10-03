/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
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

package com.dragons.aurora.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationAdapter;
import com.aurelhubert.ahbottomnavigation.notification.AHNotification;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.Aurora;
import com.dragons.aurora.OnBackPressListener;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.LoginActivity;
import com.dragons.aurora.adapters.ViewPagerAdapter;
import com.dragons.aurora.dialogs.GenericDialog;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.custom.AdaptiveToolbar;
import com.dragons.custom.CustomViewPager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;

import static com.dragons.aurora.Util.isConnected;

public class ContainerFragment extends Fragment implements UpdatableAppsFragment.OnUpdateListener {

    @BindView(R.id.viewpager)
    CustomViewPager mViewPager;
    @BindView(R.id.adtb)
    AdaptiveToolbar mAdaptiveToolbar;
    @BindView(R.id.navigation)
    AHBottomNavigation bottomNavigationView;

    private ViewPagerAdapter mViewPagerAdapter;

    public ContainerFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        return inflater.inflate(R.layout.fragment_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);
        if (Prefs.getBoolean(getContext(), Aurora.PREFERENCE_SWIPE_PAGES))
            mViewPager.setScroll(true);
        else
            mViewPager.setScroll(false);
        mViewPager.setOffscreenPageLimit(4);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                bottomNavigationView.setCurrentItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        setupBottomNav();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Accountant.isLoggedIn(getContext())) {
            if (isConnected(getContext())) {
                setUser();
            }
        } else {
            resetUser();
            askLogin();
        }
    }

    private void askLogin() {
        if (!Prefs.getBoolean(getContext(), Aurora.LOGIN_PROMPTED)) {
            Prefs.putBoolean(getContext(), Aurora.LOGIN_PROMPTED, true);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("login");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
            GenericDialog mDialog = new GenericDialog();
            mDialog.setDialogTitle(getString(R.string.action_login));
            mDialog.setDialogMessage(getString(R.string.header_usr_noEmail));
            mDialog.setPositiveButton(getString(R.string.action_login), v -> {
                Prefs.putBoolean(getContext(), Aurora.LOGIN_PROMPTED, false);
                mDialog.dismiss();
                getContext().startActivity(new Intent(getContext(), LoginActivity.class));
            });
            mDialog.show(ft, "login");
        }
    }

    private void setUser() {
        if (Accountant.isGoogle(getContext())) {
            Glide
                    .with(getContext())
                    .load(PreferenceFragment.getString(getActivity(), "GOOGLE_URL"))
                    .apply(new RequestOptions()
                            .placeholder(ContextCompat.getDrawable(getContext(), R.drawable.ic_user_placeholder))
                            .circleCrop())
                    .into(mAdaptiveToolbar.getProfileIcon());
        } else {
            (mAdaptiveToolbar.getProfileIcon()).setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_dummy_avatar));
        }
    }

    private void resetUser() {
        (mAdaptiveToolbar.getProfileIcon()).setImageDrawable(getResources()
                .getDrawable(R.drawable.ic_user_placeholder));
    }

    private void setupBottomNav() {
        int[] tabColors = getResources().getIntArray(R.array.tab_colors);
        AHBottomNavigationAdapter navigationAdapter = new AHBottomNavigationAdapter(getActivity(), R.menu.main_menu);
        navigationAdapter.setupWithBottomNavigation(bottomNavigationView, tabColors);
        if (Prefs.getBoolean(getContext(), Aurora.PREFERENCE_COLOR_NAV))
            bottomNavigationView.setColored(true);
        else {
            bottomNavigationView.setDefaultBackgroundColor(Util.getStyledAttribute(getContext(), android.R.attr.colorPrimary));
            bottomNavigationView.setAccentColor(Util.getStyledAttribute(getContext(), R.attr.colorAccent));
        }
        bottomNavigationView.setBehaviorTranslationEnabled(true);
        bottomNavigationView.setTranslucentNavigationEnabled(true);
        bottomNavigationView.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
        bottomNavigationView.setCurrentItem(0);
        bottomNavigationView.setOnTabSelectedListener((position, wasSelected) -> {
            switch (position) {
                case 0:
                    mViewPager.setCurrentItem(0, true);
                    return true;
                case 1:
                    mViewPager.setCurrentItem(1, true);
                    return true;
                case 2:
                    mViewPager.setCurrentItem(2, true);
                    return true;
                case 3:
                    mViewPager.setCurrentItem(3, true);
                    return true;
                case 4:
                    mViewPager.setCurrentItem(4, true);
                    return true;
            }
            return true;
        });
    }

    public boolean onBackPressed() {
        OnBackPressListener currentFragment = (OnBackPressListener) mViewPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
        return currentFragment != null && currentFragment.onBackPressed();
    }

    @Override
    public void setUpdateCount(int count) {
        if (count > 0) {
            AHNotification notification = new AHNotification.Builder()
                    .setText(String.valueOf(count))
                    .build();
            bottomNavigationView.setNotification(notification, 2);
        }
    }
}
