/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
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

package com.dragons.aurora.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.dragons.aurora.CategoryManager;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.CategoryAppsFragment;

public class CategoryAppsActivity extends AuroraActivity {

    static private final String INTENT_CATEGORY_ID = "INTENT_CATEGORY_ID";
    private String categoryId;

    static public Intent start(Context context, String categoryId) {
        Intent intent = new Intent(context, CategoryAppsActivity.class);
        intent.putExtra(INTENT_CATEGORY_ID, categoryId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_alt);
        onNewIntent(getIntent());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String newCategoryId = intent.getStringExtra(INTENT_CATEGORY_ID);
        if (null == newCategoryId) {
            Log.w(getClass().getSimpleName(), "No category id");
            return;
        }
        if (null == categoryId || !newCategoryId.equals(categoryId)) {
            categoryId = newCategoryId;
            setTitle(new CategoryManager(this).getCategoryName(categoryId));
            getCategoryApps(categoryId);
        }
        getCategoryApps(categoryId);
    }

    public void getCategoryApps(String categoryId) {
        CategoryAppsFragment categoryAppsFragment = new CategoryAppsFragment();
        Bundle arguments = new Bundle();
        arguments.putString("CategoryId", categoryId);
        categoryAppsFragment.setArguments(arguments);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, categoryAppsFragment).commit();
    }
}
