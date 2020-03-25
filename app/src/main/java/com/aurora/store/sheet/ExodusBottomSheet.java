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

package com.aurora.store.sheet;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.adapter.ExodusAdapter;
import com.aurora.store.model.ExodusTracker;
import com.aurora.store.model.Report;
import com.aurora.store.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ExodusBottomSheet extends BaseBottomSheet {

    public static final String TAG = "EXODUS_SHEET";
    private static final String BASE_URL = "https://reports.exodus-privacy.eu.org/reports/";

    @BindView(R.id.exodus_recycler)
    RecyclerView recyclerView;
    @BindView(R.id.btn_report)
    Button btn_report;
    @BindView(R.id.exodus_app_detail)
    TextView exodus_app_detail;
    @BindView(R.id.exodus_app_version)
    TextView exodus_app_version;

    private Report report;

    public ExodusBottomSheet() {
    }

    @Nullable
    @Override
    public View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_exodus, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onContentViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            Bundle bundle = getArguments();
            stringExtra = bundle.getString(Constants.STRING_EXTRA);
            report = gson.fromJson(stringExtra, Report.class);
            populateData();
        } else {
            dismissAllowingStateLoss();
        }
    }

    @OnClick(R.id.btn_report)
    public void viewReport() {
        requireContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BASE_URL + report.getId())));
    }

    private void populateData() {
        if (report != null) {
            exodus_app_detail.setText(report.getVersion());
            exodus_app_version.setText(StringUtils.joinWith(".", report.getVersion(), report.getVersionCode()));
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
            recyclerView.setAdapter(new ExodusAdapter(getContext(), getTrackerData(report)));
        } else {
            dismissAllowingStateLoss();
        }
    }

    private List<ExodusTracker> getTrackerData(Report report) {
        final List<ExodusTracker> exodusTrackers = new ArrayList<>();
        final ArrayList<JSONObject> trackerObjects = getTrackerObjects(report.getTrackers());
        for (JSONObject obj : trackerObjects) {
            ExodusTracker exodusTracker = null;
            try {
                exodusTracker = new ExodusTracker(
                        obj.getString("name"),
                        obj.getString("website"),
                        obj.getString("code_signature"),
                        obj.getString("creation_date"));
            } catch (Exception ignored) {
            }
            if (exodusTracker != null)
                exodusTrackers.add(exodusTracker);
        }
        return exodusTrackers;
    }

    private ArrayList<JSONObject> getTrackerObjects(List<Integer> trackerIdList) {
        try {
            InputStream inputStream = requireContext().getAssets().open("exodus_trackers.json");
            byte[] mByte = new byte[inputStream.available()];
            inputStream.read(mByte);
            inputStream.close();
            String json = new String(mByte, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(json);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            ArrayList<JSONObject> trackerObjects = new ArrayList<>();
            try {
                for (Integer trackerId : trackerIdList)
                    trackerObjects.add(jsonObject.getJSONObject(String.valueOf(trackerId)));
            } catch (Exception ignored) {
            }
            return trackerObjects;
        } catch (IOException | JSONException e) {
            Log.i(e.getMessage());
            return new ArrayList<>();
        }
    }
}
