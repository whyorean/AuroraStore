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

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dragons.aurora.CircleTransform;
import com.dragons.aurora.R;
import com.squareup.picasso.Picasso;

public class AboutFragment extends UtilFragment {

    private View v;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (v != null) {
            if ((ViewGroup) v.getParent() != null)
                ((ViewGroup) v.getParent()).removeView(v);
            return v;
        }
        v = inflater.inflate(R.layout.app_abt_inc, container, false);

        getActivity().setTitle(R.string.action_about);

        drawVersion();
        drawActions();
        drawDevCard(R.string.dev1_imgURL, (ImageView) v.findViewById(R.id.dev1_avatar));
        drawDevCard(R.string.dev2_imgURL, (ImageView) v.findViewById(R.id.dev2_avatar));
        drawList(getResources().getStringArray(R.array.contributors), ((TextView) v.findViewById(R.id.contributors)));
        drawList(getResources().getStringArray(R.array.opensource), ((TextView) v.findViewById(R.id.opensource)));

        return v;
    }

    private void drawVersion() {
        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            ((TextView) v.findViewById(R.id.app_version)).setText(packageInfo.versionName + "." + packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void drawActions() {
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        ((TextView) v.findViewById(R.id.github)).setOnClickListener(v -> {
            browserIntent.setData(Uri.parse(getResources().getString(R.string.linkGit)));
            startActivity(browserIntent);
        });
        ((TextView) v.findViewById(R.id.xda)).setOnClickListener(v -> {
            browserIntent.setData(Uri.parse(getResources().getString(R.string.linkXDA)));
            startActivity(browserIntent);
        });
        ((TextView) v.findViewById(R.id.telegram)).setOnClickListener(v -> {
            browserIntent.setData(Uri.parse(getResources().getString(R.string.linkTelegram)));
            startActivity(browserIntent);
        });
    }

    private void drawDevCard(int URL, ImageView imageView) {
        Picasso.with(this.getActivity())
                .load(getResources().getString(URL))
                .placeholder(R.drawable.ic_user_placeholder)
                .transform(new CircleTransform())
                .into(imageView);
    }

    private void drawList(String[] List, TextView tv) {
        StringBuilder builder = new StringBuilder();
        for (String s : List) {
            builder.append("◉  ");
            builder.append(s);
            builder.append("\n");
        }
        (tv).setText(builder.toString().trim());
    }
}