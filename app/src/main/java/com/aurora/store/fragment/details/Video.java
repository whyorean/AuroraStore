package com.aurora.store.fragment.details;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;
import com.aurora.store.utility.Log;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.android.volley.VolleyLog.TAG;

public class Video extends AbstractHelper {

    @BindView(R.id.app_video)
    RelativeLayout video_layout;
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
        video_layout.setVisibility(View.VISIBLE);

        video_play.setOnClickListener(v -> {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(app.getVideoUrl())));
            } catch (ActivityNotFoundException e) {
                Log.i("Something is wrong with WebView");
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
                            GlideApp
                                    .with(context)
                                    .load(obj.getString("thumbnail_url"))
                                    .transforms(new CenterCrop(), new RoundedCorners(15))
                                    .transition(new DrawableTransitionOptions().crossFade())
                                    .into(video_thumbnail);
                    } catch (Exception e) {
                        Log.e(e.getMessage());
                    }
                }, error -> VolleyLog.d(TAG, "Error: " + error.getMessage()));
        mRequestQueue.add(jsonObjReq);
    }
}