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

package com.aurora.store;

import android.content.Context;

import com.aurora.store.utility.Util;

import java.util.Random;

public class TokenDispenserMirrors {

    static private String[] mirrors = new String[]{
            "http://www.auroraoss.com:8080",
            "http://www.auroraoss.com:8880",
            "http://www.auroraoss.com:2095"
    };

    public String get(Context context) {
        if (Util.isCustomTokenizerEnabled(context))
            return Util.getCustomTokenizerURL(context);
        else
            return mirrors[new Random().nextInt(mirrors.length)];
    }
}
