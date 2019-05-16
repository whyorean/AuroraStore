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

public class TokenDispenserMirrors {

    static private String[] mirrors = new String[]{
            "http://www.auroraoss.com:8080",
            "https://token-dispenser.herokuapp.com",
            "https://token-dispenser-mirror.herokuapp.com",
            "http://token-dispenser.duckdns.org:8080"
    };

    private int n = 0;

    private void reset() {
        n = 0;
    }

    public String get() {
        if (n >= mirrors.length) {
            reset();
        }
        return mirrors[n++];
    }
}
