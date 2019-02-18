package com.aurora.store.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.aurora.store.utility.Accountant;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.manager.SpoofManager;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.view.PropertyView;
import com.dragons.aurora.playstoreapiv2.PropertiesDeviceInfoProvider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DeviceInfoActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.incognito_fab)
    FloatingActionButton incognito_fab;
    @BindView(R.id.device_info)
    LinearLayout root;

    private ActionBar mActionBar;
    private ThemeUtil mThemeUtil = new ThemeUtil();
    private String deviceName;
    private int deviceIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThemeUtil.onCreate(this);
        setContentView(R.layout.activity_device_info);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();

        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setDisplayShowTitleEnabled(false);
        }

        onNewIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mThemeUtil.onResume(this);
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
        deviceName = intent.getStringExtra(Constants.INTENT_DEVICE_NAME);
        deviceIndex = intent.getIntExtra(Constants.INTENT_DEVICE_INDEX, 0);
        if (TextUtils.isEmpty(deviceName)) {
            Log.e("No device name given");
            finish();
            return;
        }

        Properties properties = new SpoofManager(this).getProperties(deviceName);
        mToolbar.setTitle(properties.getProperty("UserReadableName"));
        List<String> keys = new ArrayList<>();
        for (Object key : properties.keySet()) {
            keys.add((String) key);
        }

        Collections.sort(keys);
        for (String key : keys) {
            addCards(root, key, ((String) properties.get(key)).replace(",", ", "));
        }

        setupButtons();
    }

    private void addCards(LinearLayout root, String key, String value) {
        root.addView(new PropertyView(this, key, value));
    }

    private void setupButtons() {
        incognito_fab = findViewById(R.id.incognito_fab);
        incognito_fab.show();
        incognito_fab.setOnClickListener(click -> showConfirmationDialog());
    }

    private boolean isDeviceDefinitionValid(String spoofDevice) {
        PropertiesDeviceInfoProvider deviceInfoProvider = new PropertiesDeviceInfoProvider();
        deviceInfoProvider.setProperties(new SpoofManager(this).getProperties(spoofDevice));
        deviceInfoProvider.setLocaleString(Locale.getDefault().toString());
        return deviceInfoProvider.isValid();
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.pref_device_to_pretend_to_be_toast)
                .setTitle(R.string.dialog_title_logout)
                .setPositiveButton(R.string.action_logout, (dialogInterface, i) -> {
                    if (!TextUtils.isEmpty(deviceName) && !isDeviceDefinitionValid(deviceName)) {
                        ContextUtil.toast(this, R.string.error_invalid_device_definition);
                    } else {
                        PrefUtil.putString(this, Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE, deviceName);
                        PrefUtil.putInteger(this, Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX, deviceIndex);
                    }
                    Accountant.completeCheckout(this);
                    dialogInterface.dismiss();
                    startActivity(new Intent(this, AccountsActivity.class));
                    this.finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
