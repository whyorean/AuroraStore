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

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.adapter.ExodusAdapter;
import com.aurora.store.model.ExodusTracker;
import com.aurora.store.model.Report;
import com.aurora.store.util.Log;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

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

public class ExodusBottomSheet extends BottomSheetDialogFragment {

    @BindView(R.id.exodus_recycler)
    RecyclerView recyclerView;
    @BindView(R.id.btn_report)
    Button btn_report;
    @BindView(R.id.exodus_app_detail)
    TextView exodus_app_detail;
    @BindView(R.id.exodus_app_version)
    TextView exodus_app_version;

    private Report report;

    public ExodusBottomSheet(Report report) {
        this.report = report;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null)
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
        });
        return dialog;
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
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(new ExodusAdapter(getContext(), getTrackerData(report)));

        exodus_app_detail.setText(report.getVersion());
        StringBuilder sb = new StringBuilder();
        sb.append("v");
        sb.append(report.getVersion());
        sb.append(".");
        sb.append(report.getVersionCode());
        exodus_app_version.setText(sb);

        btn_report.setOnClickListener(v -> requireContext().startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://reports.exodus-privacy.eu.org/reports/" + report.getId() + "/"))));
    }

    private List<ExodusTracker> getTrackerData(Report report) {
        List<ExodusTracker> exodusTrackers = new ArrayList<>();
        ArrayList<JSONObject> trackerObjects = getTrackerObjects(report.getTrackers());
        for (JSONObject obj : trackerObjects) {
            ExodusTracker exodusTracker = null;
            try {
                exodusTracker = new ExodusTracker(
                        obj.getString("name"),
                        obj.getString("website"),
                        obj.getString("code_signature"),
                        obj.getString("creation_date"));
            } catch (JSONException | NullPointerException ignored) {
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
