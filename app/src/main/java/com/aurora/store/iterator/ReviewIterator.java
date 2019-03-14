/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
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

package com.aurora.store.iterator;

import android.content.Context;

import com.aurora.store.model.Review;

import java.util.Iterator;
import java.util.List;

abstract public class ReviewIterator implements Iterator<List<Review>> {

    protected String packageName;
    protected Context context;

    protected int page = -1;

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
