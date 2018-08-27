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

package com.dragons.aurora.fragment.details;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.model.App;

public class ButtonRedirect extends Button {

    public ButtonRedirect(Context context, View view, App app) {
        super(context, view, app);
    }

    @Override
    protected android.widget.Button getButton() {
        if (view.findViewById(R.id.showInPlayStore).getVisibility() == View.VISIBLE)
            return null;
        else
            return (android.widget.Button) view.findViewById(R.id.showInPlayStore);
    }

    @Override
    protected boolean shouldBeVisible() {
        return (app.getPrice() != null && !app.isFree() && !Util.isAlreadyInstalled(context, app.getPackageName()));
    }

    @Override
    protected void onButtonClick(View v) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://video_play.google.com/store/apps/details?id=" + app.getPackageName()));
        Intent chooser = Intent.createChooser(intent, context.getString(R.string.details_run_with));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(chooser);
        }
    }
}
