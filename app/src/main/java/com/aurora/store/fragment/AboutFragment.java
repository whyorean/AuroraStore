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

package com.aurora.store.fragment;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aurora.store.R;
import com.aurora.store.view.LinkView;

import butterknife.BindView;
import butterknife.ButterKnife;


public class AboutFragment extends Fragment {

    @BindView(R.id.app_version)
    TextView txtVersion;
    @BindView(R.id.linkContainer)
    LinearLayout linkContainer;

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        drawVersion();
        drawLinks();
    }

    private void drawVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            txtVersion.setText(new StringBuilder()
                    .append("v")
                    .append(packageInfo.versionName)
                    .append(".")
                    .append(packageInfo.getLongVersionCode()));
        } catch (Exception ignored) {
        }
    }

    private void drawLinks() {
        String[] linkURLS = getResources().getStringArray(R.array.linkURLS);
        String[] linkTitles = getResources().getStringArray(R.array.linkTitles);
        String[] linkSummary = getResources().getStringArray(R.array.linkSummary);
        int[] linkIcons = {
                R.drawable.ic_paypal,
                R.drawable.ic_gitlab,
                R.drawable.ic_xda,
                R.drawable.ic_telegram,
                R.drawable.ic_fdroid
        };
        for (int i = 0; i < linkURLS.length; i++)
            linkContainer.addView(new LinkView(getContext(),
                    linkURLS[i],
                    linkTitles[i],
                    linkSummary[i],
                    linkIcons[i]));
    }
}
