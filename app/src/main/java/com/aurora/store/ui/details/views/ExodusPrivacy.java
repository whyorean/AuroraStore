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

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.model.ExodusReport;
import com.aurora.store.model.Report;
import com.aurora.store.sheet.ExodusBottomSheet;
import com.aurora.store.task.ExodusTask;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
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

    public ExodusPrivacy(DetailsActivity activity, App app) {
        super(activity, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, activity);
        show(R.id.exodus_card);
        get(EXODUS_PATH + app.getPackageName());
    }

    private void get(String url) {
        Observable.fromCallable(() -> new ExodusTask(context)
                .get(url))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(this::parseResponse)
                .doOnError(throwable -> Log.e(throwable.getMessage()))
                .subscribe();
    }

    private void parseResponse(String response) {
        try {
            final JSONObject jsonObject = new JSONObject(response);
            final JSONObject exodusObject = jsonObject.getJSONObject(app.getPackageName());
            final ExodusReport exodusReport = gson.fromJson(exodusObject.toString(), ExodusReport.class);
            final List<Report> reportList = exodusReport.getReports();
            Collections.sort(reportList, (Report1, Report2) -> Report2.getCreationDate().compareTo(Report1.getCreationDate()));
            report = reportList.get(0);
        } catch (Exception e) {
            Log.d(e.getMessage());
        } finally {
            drawExodus();
        }
    }

    private void drawExodus() {
        if (report != null) {
            if (report.getTrackers().size() > 0) {
                setText(R.id.exodus_description, context.getString(R.string.exodus_hasTracker)
                        + StringUtils.SPACE
                        + ":"
                        + StringUtils.SPACE
                        + report.getTrackers().size());
                moreButton.setEnabled(true);
                moreButton.setOnClickListener(v -> showBottomDialog());
            } else {
                setText(R.id.exodus_description, R.string.exodus_noTracker);
            }
        } else {
            setText(R.id.exodus_description, R.string.exodus_noReport);
        }
    }

    private void showBottomDialog() {
        final Bundle bundle = new Bundle();
        bundle.putString(Constants.STRING_EXTRA, gson.toJson(report));
        ExodusBottomSheet bottomSheet = new ExodusBottomSheet();
        bottomSheet.setArguments(bundle);
        bottomSheet.show(activity.getSupportFragmentManager(), ExodusBottomSheet.TAG);
    }
}
