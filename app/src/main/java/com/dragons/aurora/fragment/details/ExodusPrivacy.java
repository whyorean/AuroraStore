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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.adapters.ExodusAdapter;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.ExodusTracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.android.volley.VolleyLog.TAG;

public class ExodusPrivacy extends AbstractHelper {

    @BindView(R.id.exodus_card)
    RelativeLayout exodus_card;
    @BindView(R.id.moreButton)
    Button moreButton;

    private String AppID;
    private JSONArray trackersIDs;

    public ExodusPrivacy(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, view);
        try {
            getExodusReport(fragment.getActivity(), "https://reports.exodus-privacy.eu.org/api/search/" + app.getPackageName());
        } catch (NullPointerException e) {
            Timber.i("Probably App Switched");
        }
    }

    private void getExodusReport(Context context, String EXODUS_PATH) {
        RequestQueue mRequestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                EXODUS_PATH, null, response -> {
            try {
                JSONObject exodusReport = response.getJSONObject(app.getPackageName());
                JSONArray reportsArray = exodusReport.getJSONArray("reports");
                JSONObject trackersReport = reportsArray.getJSONObject(0);
                trackersIDs = trackersReport.getJSONArray("trackers");
                AppID = trackersReport.getString("id");
                drawExodus(trackersIDs);
            } catch (JSONException e) {
                Timber.i("Error occurred at Exodus Privacy");
            }
        }, error -> VolleyLog.d(TAG, "Error: " + error.getMessage()));
        mRequestQueue.add(jsonObjReq);
    }

    private void drawExodus(JSONArray appTrackers) {
        if (fragment.getActivity() != null) {
            exodus_card.setVisibility(View.VISIBLE);
            if (appTrackers.length() > 0) {
                setText(view, R.id.exodus_description, R.string.exodus_hasTracker, appTrackers.length());
            } else {
                setText(view, R.id.exodus_description, R.string.exodus_noTracker);
            }

            if (trackersIDs.isNull(0))
                moreButton.setVisibility(View.GONE);
            else
                moreButton.setOnClickListener(v -> showDialog(AbstractHelper.color));
        }
    }

    private void showDialog(int color) {
        Dialog ad = new Dialog(context);
        ad.setContentView(R.layout.dialog_exodus);
        ad.setCancelable(true);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(ad.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER;

        ad.getWindow().setAttributes(layoutParams);

        if (Prefs.getBoolean(context, "PREFERENCE_COLOR_UI")) {
            ImageView mImageView = ad.findViewById(R.id.exodus_img_bg);
            mImageView.setBackgroundColor(color);
            Bitmap mBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
            mBitmap.eraseColor(color);
            getPalette(mBitmap, ad);
        }

        RecyclerView mRecyclerView = ad.findViewById(R.id.exodus_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        mRecyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(context, R.anim.anim_falldown));
        DividerItemDecoration itemDecorator = new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(context.getResources().getDrawable(R.drawable.list_divider));
        mRecyclerView.addItemDecoration(itemDecorator);
        mRecyclerView.setAdapter(new ExodusAdapter(context, getTrackerData(trackersIDs)));

        Button btn_report = ad.findViewById(R.id.btn_report);
        Button btn_close = ad.findViewById(R.id.btn_close);

        btn_report.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://reports.exodus-privacy.eu.org/reports/" + AppID + "/"))));
        btn_close.setOnClickListener(v -> ad.dismiss());

        ad.show();
    }

    private void getPalette(Bitmap bitmap, Dialog ad) {
        Palette.from(bitmap).generate(palette ->
        {
            Palette.Swatch mSwatch = palette.getDominantSwatch();
            if (mSwatch != null) {
                ad.findViewById(R.id.action_container).setBackgroundColor(mSwatch.getRgb());
                ad.findViewById(R.id.div3).setBackgroundColor(mSwatch.getBodyTextColor());
                ((Button) ad.findViewById(R.id.btn_report)).setTextColor(mSwatch.getBodyTextColor());
                ((Button) ad.findViewById(R.id.btn_close)).setTextColor(mSwatch.getBodyTextColor());
            }
        });
    }

    private List<ExodusTracker> getTrackerData(JSONArray trackersIDs) {
        List<ExodusTracker> mExodusTrackers = new ArrayList<>();
        ArrayList<JSONObject> trackerObjects = getTrackerObjects(Util.getStringArray(trackersIDs));

        for (JSONObject obj : trackerObjects) {
            ExodusTracker mExodusTracker = null;
            try {
                mExodusTracker = new ExodusTracker(
                        obj.getString("name"),
                        obj.getString("website"),
                        obj.getString("code_signature"),
                        obj.getString("creation_date"));
            } catch (JSONException e) {
                Timber.e(e.getMessage());
            }
            if (mExodusTracker != null)
                mExodusTrackers.add(mExodusTracker);
        }
        return mExodusTrackers;
    }

    private ArrayList<JSONObject> getTrackerObjects(String[] IDs) {
        ArrayList<JSONObject> trackerObjects = new ArrayList<>();
        for (String ID : IDs) {
            trackerObjects.add(getOfflineTrackerObj(ID));
        }
        return trackerObjects;
    }

    private JSONObject getOfflineTrackerObj(String trackerID) {
        String ExodusJSON = null;
        try {
            InputStream mInputStream = context.getAssets().open("exodus_trackers.json");
            byte[] mByte = new byte[mInputStream.available()];
            mInputStream.read(mByte);
            mInputStream.close();
            ExodusJSON = new String(mByte, "UTF-8");
            JSONArray mJsonArray = new JSONArray(ExodusJSON);
            JSONObject mJsonObject = mJsonArray.getJSONObject(0);
            return mJsonObject.getJSONObject(trackerID);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
