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

import android.content.Context;
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

import com.aurora.store.R;
import com.aurora.store.adapter.ExodusAdapter;
import com.aurora.store.model.ExodusReport;
import com.aurora.store.model.ExodusTracker;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.Util;
import com.aurora.store.view.CustomBottomSheetDialogFragment;

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

public class ExodusBottomSheet extends CustomBottomSheetDialogFragment {

    @BindView(R.id.exodus_recycler)
    RecyclerView mRecyclerView;
    @BindView(R.id.btn_report)
    Button btn_report;
    @BindView(R.id.exodus_app_detail)
    TextView exodus_app_detail;

    private Context context;
    private ExodusReport mReport;

    public ExodusBottomSheet(ExodusReport mReport) {
        this.mReport = mReport;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_exodus, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        mRecyclerView.setAdapter(new ExodusAdapter(getContext(), getTrackerData(mReport.getTrackerIds())));

        StringBuilder sb = new StringBuilder();
        sb.append(mReport.getApp().getPackageName());
        sb.append(" | ");
        sb.append("v");
        sb.append(mReport.getVersion());
        sb.append(".");
        sb.append(mReport.getVersionCode());
        exodus_app_detail.setText(sb);

        btn_report.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://reports.exodus-privacy.eu.org/reports/" + mReport.getAppId() + "/"))));
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
            } catch (JSONException | NullPointerException ignored) {
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
        try {
            InputStream mInputStream = context.getAssets().open("exodus_trackers.json");
            byte[] mByte = new byte[mInputStream.available()];
            mInputStream.read(mByte);
            mInputStream.close();
            String ExodusJSON = new String(mByte, StandardCharsets.UTF_8);
            JSONArray mJsonArray = new JSONArray(ExodusJSON);
            JSONObject mJsonObject = mJsonArray.getJSONObject(0);
            return mJsonObject.getJSONObject(trackerID);
        } catch (IOException | JSONException e) {
            Log.i(e.getMessage());
            return null;
        }
    }
}
