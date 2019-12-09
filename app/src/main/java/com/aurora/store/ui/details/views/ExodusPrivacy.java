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

package com.aurora.store.ui.details.views;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.model.ExodusReport;
import com.aurora.store.model.Report;
import com.aurora.store.sheet.ExodusBottomSheet;
import com.aurora.store.task.NetworkTask;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.util.Log;
import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ExodusPrivacy extends AbstractDetails {

    private static final String EXODUS_PATH = "https://reports.exodus-privacy.eu.org/api/search/";

    @BindView(R.id.root_layout)
    LinearLayout rootLayout;
    @BindView(R.id.exodus_card)
    RelativeLayout exodus_card;
    @BindView(R.id.moreButton)
    Button moreButton;

    private Report report;
    private CompositeDisposable disposable = new CompositeDisposable();

    public ExodusPrivacy(DetailsActivity activity, App app) {
        super(activity, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, activity);
        get(EXODUS_PATH + app.getPackageName());
    }

    private void get(String url) {
        disposable.add(Observable.fromCallable(() -> new NetworkTask(context)
                .get(url))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> parseResponse(response)));
    }

    private void parseResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject exodusObject = jsonObject.getJSONObject(app.getPackageName());
            Gson gson = new Gson();
            ExodusReport exodusReport = gson.fromJson(exodusObject.toString(), ExodusReport.class);
            List<Report> reportList = exodusReport.getReports();
            Collections.sort(reportList, (Report1, Report2) -> Report2.getCreationDate().compareTo(Report1.getCreationDate()));
            report = reportList.get(0);
        } catch (Exception e) {
            Log.d(e.getMessage());
        } finally {
            drawExodus();
        }
    }

    private void drawExodus() {
        show(rootLayout, R.id.exodus_card);

        if (report == null) {
            setText(R.id.exodus_description, R.string.exodus_noReport);
            moreButton.setVisibility(View.GONE);
            return;
        }

        if (report.getTrackers().size() > 0) {
            setText(R.id.exodus_description, new StringBuilder()
                    .append(context.getString(R.string.exodus_hasTracker))
                    .append(StringUtils.SPACE)
                    .append(report.getTrackers().size()).toString());
        } else {
            setText(R.id.exodus_description, R.string.exodus_noTracker);
        }

        if (report.getTrackers().isEmpty())
            moreButton.setVisibility(View.GONE);
        else
            moreButton.setOnClickListener(v -> showBottomDialog());
    }

    private void showBottomDialog() {
        ExodusBottomSheet bottomSheet = new ExodusBottomSheet(report);
        bottomSheet.show(activity.getSupportFragmentManager(), "EXODUS");
    }
}
