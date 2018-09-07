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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.dragons.aurora.R;
import com.dragons.aurora.model.App;

import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;

public abstract class Abstract {
    protected View view;
    protected App app;
    protected Context context;


    @BindView(R.id.viewSwitcher)
    @Nullable
    ViewSwitcher mViewSwitcher;
    @BindView(R.id.view1)
    @Nullable
    LinearLayout actions_layout;
    @BindView(R.id.view2)
    @Nullable
    LinearLayout progress_layout;
    @BindView(R.id.download_progress)
    @Nullable
    ProgressBar progressBar;
    @BindView(R.id.download_progress_txt)
    @Nullable
    TextView progressCents;

    public Abstract(Context context, View view, App app) {
        this.context = context;
        this.view = view;
        this.app = app;
        ButterKnife.bind(this, view);
    }

    abstract public void draw();

    protected void setText(int viewId, String text) {
        TextView textView = view.findViewById(viewId);
        if (null != textView)
            textView.setText(text);
    }

    protected void setText(int viewId, int stringId, Object... text) {
        setText(viewId, view.getResources().getString(stringId, text));
    }
}
