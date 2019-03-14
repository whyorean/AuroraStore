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

package com.aurora.store.fragment.details;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BackToPlayStore extends AbstractHelper {

    static private final String PLAY_STORE_PACKAGE_NAME = "com.android.vending";

    @BindView(R.id.to_play_store)
    ImageView toPlayStore;

    public BackToPlayStore(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, view);
        if (!isPlayStoreInstalled() || !app.isInPlayStore()) {
            return;
        }
        toPlayStore.setVisibility(View.VISIBLE);
        toPlayStore.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(Constants.APP_DETAIL_URL + app.getPackageName()));
            context.startActivity(i);
        });
    }

    private boolean isPlayStoreInstalled() {
        try {
            return null != context.getPackageManager().getPackageInfo(PLAY_STORE_PACKAGE_NAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}