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

package com.dragons.aurora.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.details.AppLists;
import com.dragons.aurora.fragment.details.BackToPlayStore;
import com.dragons.aurora.fragment.details.Beta;
import com.dragons.aurora.fragment.details.DownloadOptions;
import com.dragons.aurora.fragment.details.DownloadOrInstall;
import com.dragons.aurora.fragment.details.ExodusPrivacy;
import com.dragons.aurora.fragment.details.GeneralDetails;
import com.dragons.aurora.fragment.details.Permissions;
import com.dragons.aurora.fragment.details.Review;
import com.dragons.aurora.fragment.details.Screenshot;
import com.dragons.aurora.fragment.details.Share;
import com.dragons.aurora.fragment.details.SystemAppPage;
import com.dragons.aurora.fragment.details.Video;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.playstore.DetailsAppTaskHelper;
import com.percolate.caffeine.ToastUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.dragons.aurora.Util.hide;
import static com.dragons.aurora.Util.isConnected;
import static com.dragons.aurora.Util.show;

public class DetailsFragment extends DetailsAppTaskHelper {

    public static App app;

    private View view;
    private DownloadOrInstall downloadOrInstallFragment;
    private String packageName;
    private Disposable disposable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view != null) {
            if ((ViewGroup) view.getParent() != null)
                ((ViewGroup) view.getParent()).removeView(view);
            return view;
        }
        view = inflater.inflate(R.layout.fragment_details, container, false);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            packageName = arguments.getString("PackageName");
            if (isConnected(getContext()) && Accountant.isLoggedIn(getContext()))
                fetchDetails();
            else
                ToastUtils.quickToast(getContext(), "Make sure you are Connected & Logged in");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Button retry_details = view.findViewById(R.id.ohhSnap_retry);
        retry_details.setOnClickListener(click -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext())) {
                hide(view, R.id.ohhSnap);
                show(view, R.id.progress);
                fetchDetails();
            }
        });

        ImageView action_back = view.findViewById(R.id.action_back);
        action_back.setOnClickListener(c -> getActivity().onBackPressed());
    }

    @Override
    public void onPause() {
        if (null != downloadOrInstallFragment) {
            downloadOrInstallFragment.unregisterReceivers();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (null != downloadOrInstallFragment) {
            downloadOrInstallFragment.unregisterReceivers();
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        redrawButtons();
        super.onResume();
    }

    private void fetchDetails() {
        disposable = Observable.fromCallable(() -> getResult(new PlayStoreApiAuthenticator(this.getActivity()).getApi(), packageName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(app -> {
                    DetailsFragment.app = app;
                    redrawDetails(app);
                }, err -> {
                    hide(view, R.id.progress);
                    show(view, R.id.ohhSnap);
                    processException(err);
                });
    }

    private void redrawDetails(App app) {
        new GeneralDetails(this, app).draw();
        new ExodusPrivacy(this, app).draw();
        new Permissions(this, app).draw();
        new Screenshot(this, app).draw();
        new Review(this, app).draw();
        new AppLists(this, app).draw();
        new BackToPlayStore(this, app).draw();
        new Share(this, app).draw();
        new SystemAppPage(this, app).draw();
        new Video(this, app).draw();
        new Beta(this, app).draw();

        if (null != downloadOrInstallFragment) {
            downloadOrInstallFragment.unregisterReceivers();
        }
        downloadOrInstallFragment = new DownloadOrInstall(getContext(), view, app);
        redrawButtons();
        new DownloadOptions(getContext(), view, app).draw();
    }

    private void redrawButtons() {
        if (null != downloadOrInstallFragment) {
            downloadOrInstallFragment.unregisterReceivers();
            downloadOrInstallFragment.registerReceivers();
            downloadOrInstallFragment.draw();
        }
    }
}