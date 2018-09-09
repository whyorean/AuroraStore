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

package com.dragons.aurora;

import android.content.Context;

import com.dragons.aurora.model.App;

import java.io.File;

import timber.log.Timber;

abstract public class DeltaPatcherAbstract {

    protected App app;
    protected Context context;
    protected File patch;

    public DeltaPatcherAbstract(Context context, App app) {
        Timber.i("Chosen delta patcher");
        this.app = app;
        this.context = context;
        patch = Paths.getDeltaPath(context, app.getPackageName(), app.getVersionCode());
    }

    public abstract boolean patch();
}
