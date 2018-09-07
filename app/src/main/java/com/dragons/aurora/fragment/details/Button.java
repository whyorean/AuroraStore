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
import android.content.pm.PackageManager;
import android.view.View;

import com.dragons.aurora.model.App;

public abstract class Button extends Abstract {

    protected View button;

    public Button(Context context, View view, App app) {
        super(context, view, app);
        this.button = getButton();
    }

    abstract protected View getButton();

    abstract protected boolean shouldBeVisible();

    abstract protected void onButtonClick(View v);

    @Override
    public void draw() {
        if (null == button) {
            return;
        }
        button.setEnabled(true);
        button.setVisibility(shouldBeVisible() ? View.VISIBLE : View.GONE);
        button.setOnClickListener(this::onButtonClick);
    }

    void disable(int stringId) {
        if (null == button) {
            return;
        }
        ((android.widget.Button) button).setText(stringId);
        button.setEnabled(false);
    }

    void switchViews() {
        if (mViewSwitcher.getCurrentView() == actions_layout)
            mViewSwitcher.showNext();
        else if (mViewSwitcher.getCurrentView() == progress_layout)
            mViewSwitcher.showPrevious();
    }

    protected boolean isInstalled() {
        try {
            context.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
