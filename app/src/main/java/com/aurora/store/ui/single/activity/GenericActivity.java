package com.aurora.store.ui.single.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.ui.accounts.fragment.AccountsFragment;
import com.aurora.store.ui.preference.fragment.AboutFragment;
import com.aurora.store.ui.preference.fragment.BlacklistFragment;
import com.aurora.store.ui.preference.fragment.FavouriteFragment;
import com.aurora.store.ui.preference.fragment.SpoofFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GenericActivity extends BaseActivity {

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
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String fragmentName = intent.getStringExtra(Constants.FRAGMENT_NAME);
        Fragment fragment = null;

        switch (fragmentName) {
            case Constants.FRAGMENT_ABOUT:
                actionBar.setTitle(getString(R.string.action_about));
                fragment = new AboutFragment();
                break;
            case Constants.FRAGMENT_BLACKLIST:
                actionBar.setTitle(getString(R.string.action_blacklist));
                fragment = new BlacklistFragment();
                break;
            case Constants.FRAGMENT_FAV_LIST:
                actionBar.setTitle(getString(R.string.action_favourites));
                fragment = new FavouriteFragment();
                break;
            case Constants.FRAGMENT_SPOOF:
                actionBar.setTitle(getString(R.string.action_spoofed));
                fragment = new SpoofFragment();
                break;
            default:
                actionBar.setTitle(getString(R.string.action_accounts));
                fragment = new AccountsFragment();
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
