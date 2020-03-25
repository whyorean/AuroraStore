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

package com.aurora.store.ui.single.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.model.App;
import com.aurora.store.task.DeliveryData;
import com.aurora.store.task.LiveUpdate;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Log;
import com.aurora.store.util.TextUtil;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ManualDownloadActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.icon)
    ImageView appIcon;
    @BindView(R.id.versionString)
    TextView app_version;
    @BindView(R.id.edit_text_layout)
    TextInputLayout inputLayout;
    @BindView(R.id.edit_text)
    TextInputEditText editText;
    @BindView(R.id.btn_positive)
    Button btnPositive;
    @BindView(R.id.chip_group_info)
    ChipGroup chipGroup;

    private App app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.action_manual));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            stringExtra = intent.getStringExtra(Constants.STRING_EXTRA);
            app = gson.fromJson(stringExtra, App.class);
            if (app != null) {
                populateData();
            }
        } else {
            finishAfterTransition();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void populateData() {
        Glide.with(this)
                .asBitmap()
                .load(app.getIconUrl())
                .transition(new BitmapTransitionOptions().crossFade())
                .transforms(new CenterCrop(), new RoundedCorners(50))
                .into(appIcon);

        setText(R.id.displayName, app.getDisplayName());
        setText(R.id.packageName, app.getPackageName());
        setText(R.id.devName, app.getDeveloperName());
        drawVersion();
        drawGeneralDetails();
        drawEditText();
        btnPositive.setOnClickListener(view -> downloadListener());
    }

    private void drawVersion() {
        String versionName = app.getVersionName();
        if (TextUtils.isEmpty(versionName)) {
            return;
        }
        app_version.setText(versionName);
        app_version.setVisibility(View.VISIBLE);
    }

    private void drawGeneralDetails() {
        setText(R.id.category, new CategoryManager(this).getCategoryName(app.getCategoryId()));
        final String category = new CategoryManager(this).getCategoryName(app.getCategoryId());
        if (TextUtil.isEmpty(category))
            ViewUtil.hideWithAnimation(findViewById(R.id.category));
        else
            setText(R.id.category, new CategoryManager(this).getCategoryName(app.getCategoryId()));
        if (app.getPrice() != null && app.getPrice().isEmpty())
            setText(R.id.price, R.string.category_appFree);
        else
            setText(R.id.price, app.getPrice());
        setText(R.id.contains_ads, app.isContainsAds() ? R.string.details_contains_ads : R.string.details_no_ads);
        setText(R.id.txt_rating, app.getLabeledRating());
        setText(R.id.txt_installs, Util.addDiPrefix(app.getInstalls()));
        setText(R.id.txt_size, Formatter.formatShortFileSize(this, app.getSize()));
        setText(R.id.txt_updated, app.getUpdated());
        setText(R.id.txt_google_dependencies, app.getDependencies().isEmpty()
                ? R.string.list_app_independent_from_gsf
                : R.string.list_app_depends_on_gsf);
        chipGroup.setVisibility(View.VISIBLE);
    }

    private void drawEditText() {
        inputLayout.setHint(String.valueOf(app.getVersionCode()));
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    app.setVersionCode(Integer.parseInt(s.toString()));
                } catch (NumberFormatException e) {
                    Log.w("%s is not a number", s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void downloadListener() {
        Observable.fromCallable(() -> new DeliveryData(this)
                .getDeliveryData(app))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(di -> ContextUtil.runOnUiThread(() -> {
                    btnPositive.setEnabled(false);
                    btnPositive.setText(getString(R.string.download_progress));
                }))
                .doOnTerminate(() -> ContextUtil.runOnUiThread(() -> {
                    btnPositive.setEnabled(true);
                    btnPositive.setText(getString(R.string.details_download));
                }))
                .doOnNext(androidAppDeliveryData -> {
                    new LiveUpdate(this).enqueueUpdate(app, androidAppDeliveryData);
                })
                .doOnError(throwable -> {
                    Toast.makeText(this, "Failed to download specified build", Toast.LENGTH_SHORT).show();
                })
                .subscribe();
    }

    protected void setText(int viewId, String text) {
        TextView textView = findViewById(viewId);
        if (null != textView)
            textView.setText(text);
    }

    protected void setText(int viewId, int stringId, Object... text) {
        setText(viewId, getResources().getString(stringId, text));
    }
}


