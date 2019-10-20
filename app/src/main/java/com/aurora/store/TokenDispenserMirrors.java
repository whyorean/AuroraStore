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

import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;

import java.util.ArrayList;
import java.util.List;

public class TokenDispenserMirrors {

    private static List<String> dispenserList = new ArrayList<>();

    static {
        dispenserList.add("http://auroraoss.com:8080");
        dispenserList.add("http://92.42.46.11:8080");
        dispenserList.add("http://auroraoss.in:8080");
        dispenserList.add("https://token-dispenser.calyxinstitute.org");
    }

    public static String get(Context context) {
        if (Util.isCustomTokenizerEnabled(context))
            return Util.getCustomTokenizerURL(context);
        else
            return Util.getTokenizerURL(context);
    }

    public static void setNextDispenser(Context context, int dispenserNum) {
        PrefUtil.putString(context, Constants.PREFERENCE_TOKENIZER_URL, dispenserList.get(dispenserNum % dispenserList.size()));
    }
}
