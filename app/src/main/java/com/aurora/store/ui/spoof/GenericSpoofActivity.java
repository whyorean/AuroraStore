package com.aurora.store.ui.spoof;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.ui.spoof.fragment.DeviceSpoofFragment;
import com.aurora.store.ui.spoof.fragment.GeoLocationSpoofFragment;
import com.aurora.store.ui.spoof.fragment.LocaleSpoofFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GenericSpoofActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private ActionBar actionBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic);
        ButterKnife.bind(this);
        setupActionBar();
        onNewIntent(getIntent());
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        final String fragmentName = intent.getStringExtra(Constants.FRAGMENT_NAME);
        Fragment fragment = null;

        switch (fragmentName) {
            case Constants.FRAGMENT_SPOOF_DEVICE:
                actionBar.setTitle(getString(R.string.pref_category_spoof_device));
                fragment = new DeviceSpoofFragment();
                break;
            case Constants.FRAGMENT_SPOOF_LOCALE:
                actionBar.setTitle(getString(R.string.pref_category_spoof_lang));
                fragment = new LocaleSpoofFragment();
                break;
            case Constants.FRAGMENT_SPOOF_GEOLOCATION:
                actionBar.setTitle(getString(R.string.pref_category_spoof_location));
                fragment = new GeoLocationSpoofFragment();
                break;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .commit();
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }
    }
}
