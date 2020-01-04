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

package com.aurora.store.ui.details;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.section.FilesSection;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.ui.view.MoreLayout;
import com.aurora.store.util.TextUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class ReadMoreActivity extends BaseActivity {

    public static App app;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.txt_changelog)
    AppCompatTextView txtChangelog;
    @BindView(R.id.content_readMore)
    AppCompatTextView contentReadMore;
    @BindView(R.id.layout_more)
    LinearLayout layoutMore;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_more);
        ButterKnife.bind(this);
        if (app == null) {
            finish();
            return;
        }
        setupActionBar();
        setupMore();
        setupRecycler();
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

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(app.getDisplayName());
        }
    }

    @Override
    protected void onDestroy() {
        app = null;
        super.onDestroy();
    }

    private void setupMore() {

        String changes = app.getChanges();

        if (TextUtil.isEmpty(changes))
            txtChangelog.setText(getString(R.string.details_no_changes));
        else
            txtChangelog.setText(Html.fromHtml(changes).toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            contentReadMore.setText(Html.fromHtml(app.getDescription(), Html.FROM_HTML_MODE_LEGACY).toString());
        else
            contentReadMore.setText(Html.fromHtml(app.getDescription()));

        MoreLayout m1 = new MoreLayout(this);
        m1.setLabel(getString(R.string.details_more_version));
        m1.setValue(app.getVersionName());

        MoreLayout m2 = new MoreLayout(this);
        m2.setLabel(getString(R.string.details_more_updated));
        m2.setValue(app.getUpdated());

        MoreLayout m3 = new MoreLayout(this);
        m3.setLabel(getString(R.string.menu_downloads));
        m3.setValue(app.getDownloadString());

        layoutMore.addView(m1);
        layoutMore.addView(m2);
        layoutMore.addView(m3);

        for (String key : app.getOfferDetails().keySet()) {
            MoreLayout moreLayout = new MoreLayout(this);
            moreLayout.setLabel(key);
            moreLayout.setValue(app.getOfferDetails().get(key));
            layoutMore.addView(moreLayout);
        }
    }

    public void setupRecycler() {
        if (app.getFileMetadataList() == null)
            return;

        FilesSection section = new FilesSection(this, position -> {
        });
        section.addData(app.getFileMetadataList());
        SectionedRecyclerViewAdapter adapter = new SectionedRecyclerViewAdapter();
        adapter.addSection(section);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recyclerView.setVisibility(View.VISIBLE);
    }
}
