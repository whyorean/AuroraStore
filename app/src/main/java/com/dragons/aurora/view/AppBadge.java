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
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dragons.aurora.NetworkState;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.ImageSource;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public abstract class AppBadge extends ListItem {

    protected Context context;
    protected App app;
    protected List<String> line2 = new ArrayList<>();
    protected List<String> line3 = new ArrayList<>();

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    @Override
    public void draw() {
        view.findViewById(R.id.list_container).setVisibility(View.VISIBLE);

        ((TextView) view.findViewById(R.id.text1)).setText(app.getDisplayName());
        setText(R.id.text2, TextUtils.join(" • ", line2));
        setText(R.id.text3, TextUtils.join(" • ", line3));

        if (app.isTestingProgramOptedIn())
            view.findViewById(R.id.beta_user).setVisibility(View.VISIBLE);
        if (app.isTestingProgramAvailable())
            view.findViewById(R.id.beta_avail).setVisibility(View.VISIBLE);
        if (app.isEarlyAccess())
            view.findViewById(R.id.early_access).setVisibility(View.VISIBLE);
    }

    protected void drawIcon(ImageView imageView) {
        ImageSource imageSource = app.getIconInfo();
        if (null != imageSource.getApplicationInfo() && !noImages()) {
            imageView.setImageDrawable(imageView.getContext().getPackageManager().getApplicationIcon(imageSource.getApplicationInfo()));
        } else if (!noImages()) {
            Picasso
                    .with(view.getContext())
                    .load(imageSource.getUrl())
                    .placeholder(ContextCompat.getDrawable(view.getContext(),R.drawable.ic_placeholder))
                    .into(imageView);
        }
    }

    protected void setText(int viewId, String text) {
        TextView textView = (TextView) view.findViewById(viewId);
        if (!TextUtils.isEmpty(text)) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private boolean noImages() {
        return NetworkState.isMetered(view.getContext()) && PreferenceFragment.getBoolean(view.getContext(), PreferenceFragment.PREFERENCE_NO_IMAGES);
    }
}
