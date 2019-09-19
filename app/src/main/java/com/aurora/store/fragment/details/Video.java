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
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;
import com.aurora.store.task.NetworkTask;
import com.aurora.store.utility.Log;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class Video extends AbstractHelper {

    @BindView(R.id.app_video)
    RelativeLayout video_layout;
    @BindView(R.id.thumbnail)
    ImageView video_thumbnail;
    @BindView(R.id.video_play)
    ImageView video_play;

    private CompositeDisposable disposable = new CompositeDisposable();

    public Video(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, view);
        if (TextUtils.isEmpty(app.getVideoUrl())) {
            return;
        }

        getThumb(app.getVideoUrl());
        video_layout.setVisibility(View.VISIBLE);

        video_play.setOnClickListener(v -> {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(app.getVideoUrl())));
            } catch (ActivityNotFoundException e) {
                Log.i("Something is wrong with WebView");
            }
        });
    }

    private void getThumb(String videoURL) {
        disposable.add(Observable.fromCallable(() -> new NetworkTask(context)
                .get("http://www.youtube.com/oembed?url=" + videoURL + "&format=json"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    JSONObject jsonObject = new JSONObject(response);
                    GlideApp
                            .with(context)
                            .load(jsonObject.getString("thumbnail_url"))
                            .transforms(new CenterCrop(), new RoundedCorners(25))
                            .transition(new DrawableTransitionOptions().crossFade())
                            .into(video_thumbnail);
                }, throwable -> {
                    Log.i("Error occurred at generating report");
                }));
    }
}