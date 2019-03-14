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

import android.widget.ImageView;

import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;
import com.aurora.store.sheet.PermissionBottomSheet;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Permissions extends AbstractHelper {

    @BindView(R.id.app_permissions)
    ImageView app_permissions;

    public Permissions(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, view);
        app_permissions.setOnClickListener(v -> {
            showPermissions();
        });
    }

    private void showPermissions() {
        PermissionBottomSheet profileFragment = new PermissionBottomSheet();
        profileFragment.setApp(app);
        profileFragment.show(fragment.getChildFragmentManager(), "PERMISSION");
    }
}