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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.dialogs.PaymentDialog;
import com.dragons.custom.LinkCard;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class AboutFragment extends BaseFragment {

    private final int linkIcons[] = {
            R.drawable.ic_paypal,
            R.drawable.ic_gitlab,
            R.drawable.ic_xda,
            R.drawable.ic_telegram,
            R.drawable.ic_fdroid
    };

    private View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_about, container, false);
        drawVersion();
        drawLinks();
        return view;
    }

    private void drawVersion() {
        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            ((TextView) view.findViewById(R.id.app_version)).setText(packageInfo.versionName + "." + packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void drawLinks() {
        LinearLayout linkContainer = view.findViewById(R.id.linkContainer);
        LinkCard paytmCard = new LinkCard(getContext(), "", "Paytm", "Support development,Paytm Karo!", R.drawable.ic_paytm, false);
        paytmCard.setOnClickListener(v -> {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
            PaymentDialog paymentDialog = new PaymentDialog();
            paymentDialog.show(ft, "dialog");
        });

        linkContainer.addView(paytmCard);

        String[] linkURLS = getResources().getStringArray(R.array.linkURLS);
        String[] linkTitles = getResources().getStringArray(R.array.linkTitles);
        String[] linkSummary = getResources().getStringArray(R.array.linkSummary);
        int index = 0;
        for (String URL : linkURLS)
            linkContainer.addView(new LinkCard(getContext(),
                    URL,
                    linkTitles[index],
                    linkSummary[index],
                    linkIcons[index++],
                    true));
    }
}