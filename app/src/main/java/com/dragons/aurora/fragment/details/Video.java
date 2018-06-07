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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.dragons.aurora.R;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.model.App;
import com.squareup.picasso.Picasso;

public class Video extends AbstractHelper {

    public Video(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    private String getID(String URL) {
        if (URL.contains("/youtu.be/"))
            URL = URL.substring(URL.lastIndexOf('/') + 1, URL.length());
        else if (URL.contains("feature"))
            URL = URL.substring(URL.indexOf('=') + 1, URL.lastIndexOf('&'));
        else
            URL = URL.substring(URL.indexOf('=') + 1, URL.length());
        return URL;
    }

    @Override
    public void draw() {
        if (TextUtils.isEmpty(app.getVideoUrl())) {
            return;
        }

        String vID = getID(app.getVideoUrl());
        String URL = "https://img.youtube.com/vi/" + vID + "/hqdefault.jpg";

        ImageView imageView = fragment.getActivity().findViewById(R.id.thumbnail);

        Picasso.with(fragment.getActivity())
                .load(URL)
                .fit()
                .placeholder(android.R.color.transparent)
                .centerCrop()
                .into(imageView);

        fragment.getActivity().findViewById(R.id.app_video).setVisibility(View.VISIBLE);

        ImageView play = fragment.getActivity().findViewById(R.id.vid_play);
        play.setOnClickListener(v -> {
            try {
                fragment.getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(app.getVideoUrl())));
            } catch (ActivityNotFoundException e) {
                Log.i(getClass().getSimpleName(), "Something is wrong with WebView");
            }
        });
    }
}