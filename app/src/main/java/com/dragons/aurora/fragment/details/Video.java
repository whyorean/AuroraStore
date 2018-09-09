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
import android.view.View;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.model.App;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.android.volley.VolleyLog.TAG;

public class Video extends AbstractHelper {

    @BindView(R.id.thumbnail)
    ImageView video_thumbnail;
    @BindView(R.id.video_play)
    ImageView video_play;

    public Video(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, view);
        if (TextUtils.isEmpty(app.getVideoUrl())) {
            return;
        }

        getVideoThumbURL(app.getVideoUrl());
        view.findViewById(R.id.app_video).setVisibility(View.VISIBLE);

        video_play.setOnClickListener(v -> {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(app.getVideoUrl())));
            } catch (ActivityNotFoundException e) {
                Timber.i("Something is wrong with WebView");
            }
        });
    }

    private void getVideoThumbURL(String videoURL) {
        RequestQueue mRequestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                "http://www.youtube.com/oembed?url=" + videoURL + "&format=json",
                null,
                obj -> {
                    try {
                        if (context != null)
                            Glide
                                    .with(context)
                                    .load(obj.getString("thumbnail_url"))
                                    .apply(new RequestOptions().centerCrop().placeholder(android.R.color.transparent))
                                    .transition(new DrawableTransitionOptions().crossFade())
                                    .into(video_thumbnail);
                    } catch (Exception e) {
                        Timber.e(e.getMessage());
                    }
                }, error -> VolleyLog.d(TAG, "Error: " + error.getMessage()));
        mRequestQueue.add(jsonObjReq);
    }
}