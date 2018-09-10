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

package com.dragons.aurora.fragment.details;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

import com.dragons.aurora.R;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.model.App;

public class Share extends AbstractHelper {

    static private String PLAYSTORE_LINK_PREFIX = "https://play.google.com/store/apps/details?id=";

    public Share(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        ImageView share = view.findViewById(R.id.share);
        share.setVisibility(View.VISIBLE);
        share.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, app.getDisplayName());
            i.putExtra(Intent.EXTRA_TEXT, PLAYSTORE_LINK_PREFIX + app.getPackageName());
            context.startActivity(Intent.createChooser(i, fragment.getActivity().getString(R.string.details_share)));
        });
    }
}