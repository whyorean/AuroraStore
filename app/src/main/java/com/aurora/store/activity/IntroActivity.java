package com.aurora.store.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.aurora.store.R;
import com.aurora.store.adapter.CustomViewPagerAdapter;
import com.aurora.store.fragment.AccountsFragment;
import com.aurora.store.fragment.intro.PermissionFragment;
import com.aurora.store.fragment.intro.WelcomeFragment;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.view.CustomViewPager;

import butterknife.BindView;
import butterknife.ButterKnife;

public class IntroActivity extends AppCompatActivity {

    @BindView(R.id.viewpager)
    CustomViewPager mViewPager;


    private ThemeUtil mThemeUtil = new ThemeUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThemeUtil.onCreate(this);
        setContentView(R.layout.activity_intro);
        ButterKnife.bind(this);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mThemeUtil.onResume(this);
    }

    private void init() {
        CustomViewPagerAdapter mViewPagerAdapter = new CustomViewPagerAdapter(getSupportFragmentManager());
        mViewPagerAdapter.addFragment(0, new WelcomeFragment());
        mViewPagerAdapter.addFragment(1, new PermissionFragment());
        mViewPagerAdapter.addFragment(2, new AccountsFragment());
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setPagingEnabled(false);
    }

    public void moveForward() {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
    }
}
