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

package com.dragons.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.activities.CategoryAppsActivity;

public class MoreAppsCard extends RelativeLayout {

    String category;
    String label;
    TextView card_title;
    Button more_apps;

    public MoreAppsCard(Context context, String category, String label) {
        super(context);
        this.category = category;
        this.label = label;
        init(context);
    }

    public MoreAppsCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        View view = inflate(context, R.layout.item_more, this);
        card_title = view.findViewById(R.id.m_apps_title);
        more_apps = view.findViewById(R.id.m_apps_more);
        card_title.setText(label);
        more_apps.setOnClickListener(v -> context.startActivity(CategoryAppsActivity.start(context, category)));
    }
}
