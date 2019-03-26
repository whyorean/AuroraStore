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

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;
import com.aurora.store.model.ExodusReport;
import com.aurora.store.sheet.ExodusBottomSheet;
import com.aurora.store.utility.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.android.volley.Request.Method.GET;
import static com.aurora.store.Constants.TAG;

public class ExodusPrivacy extends AbstractHelper {

    public static final String EXODUS_PATH = "https://reports.exodus-privacy.eu.org/api/search/";

    @BindView(R.id.exodus_card)
    RelativeLayout exodus_card;
    @BindView(R.id.moreButton)
    Button moreButton;

    private ExodusReport mReport;

    public ExodusPrivacy(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, view);
        try {
            getExodusReport(fragment.getActivity(), EXODUS_PATH + app.getPackageName());
        } catch (NullPointerException e) {
            Log.i("Probably App Switched");
        }
    }

    private void getExodusReport(Context context, String EXODUS_PATH) {
        RequestQueue mRequestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(GET,
                EXODUS_PATH, null, response -> {
            try {
                JSONObject exodusReport = response.getJSONObject(app.getPackageName());
                JSONArray reportsArray = exodusReport.getJSONArray("reports");
                JSONObject trackersReport = reportsArray.getJSONObject(reportsArray.length() - 1);
                mReport = new ExodusReport();
                mReport.setTrackerIds(trackersReport.getJSONArray("trackers"));
                mReport.setAppId(trackersReport.getString("id"));
                mReport.setVersion(trackersReport.getString("version"));
                mReport.setVersionCode(trackersReport.getString("version_code"));
                mReport.setApp(app);
                drawExodus(mReport);
            } catch (JSONException e) {
                Log.i("Error occurred at Exodus Privacy");
            }
        }, error -> VolleyLog.d(TAG, "Error: " + error.getMessage()));
        mRequestQueue.add(jsonObjReq);
    }

    private void drawExodus(ExodusReport mReport) {
        if (fragment.getActivity() != null) {
            exodus_card.setVisibility(View.VISIBLE);
            if (mReport.getTrackerIds().length() > 0) {
                setText(view, R.id.exodus_description,
                        new StringBuilder()
                                .append(context.getString(R.string.exodus_hasTracker))
                                .append(StringUtils.SPACE)
                                .append(mReport.getTrackerIds().length()).toString());
            } else {
                setText(view, R.id.exodus_description, R.string.exodus_noTracker);
            }

            if (mReport.getTrackerIds().isNull(0))
                moreButton.setVisibility(View.GONE);
            else
                moreButton.setOnClickListener(v -> showBottomDialog());
        }
    }

    private void showBottomDialog() {
        ExodusBottomSheet mBottomSheet = new ExodusBottomSheet(mReport);
        mBottomSheet.show(fragment.getChildFragmentManager(), "EXODUS");
    }
}
