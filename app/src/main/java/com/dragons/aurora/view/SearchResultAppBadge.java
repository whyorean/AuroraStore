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

package com.dragons.aurora.view;

import android.content.Context;
import android.widget.ImageView;

import com.dragons.aurora.R;
import com.dragons.aurora.Util;

public class SearchResultAppBadge extends AppBadge {

    @Override
    public void draw() {
        line2.clear();
        line3.clear();
        Context c = view.getContext();
        line2.add(c.getString(R.string.details_size, Util.addSiPrefix((int) app.getSize())));
        if (!app.isEarlyAccess())
            line2.add(c.getString(R.string.details_rating, (app.getRating().getAverage())) + " ★");
        line3.add(app.getPrice());
        line3.add(c.getString(app.containsAds() ? R.string.list_app_has_ads : R.string.list_app_no_ads));
        line3.add(c.getString(app.getDependencies().isEmpty() ? R.string.list_app_independent_from_gsf : R.string.list_app_depends_on_gsf));
        drawIcon((ImageView) view.findViewById(R.id.icon));
        super.draw();
    }
}
