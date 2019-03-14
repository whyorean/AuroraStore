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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;

import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;
import com.aurora.store.utility.Log;

import butterknife.BindView;

public class SystemAppPage extends AbstractHelper {

    @BindView(R.id.system_app_info)
    ImageView systemAppInfo;

    public SystemAppPage(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        if (!app.isInstalled()) {
            hide(fragment.getView(), R.id.system_app_info);
            return;
        }
        show(fragment.getView(), R.id.system_app_info);
        systemAppInfo.setOnClickListener(v -> startActivity());
    }

    private void startActivity() {
        try {
            context.startActivity(getIntent());
        } catch (ActivityNotFoundException e) {
            Log.e("Could not find system app activity");
        }
    }

    private Intent getIntent() {
        return new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:" + app.getPackageName()));
    }
}