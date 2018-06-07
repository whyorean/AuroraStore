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

public class TokenDispenserMirrors {

    static private String[] mirrors = new String[]{
            "https://token-dispenser.herokuapp.com",
            "http://route-token-dispenser.193b.starter-ca-central-1.openshiftapps.com",
            "http://token-dispenser.duckdns.org:8080"
    };
    private int n = 0;

    public void reset() {
        n = 0;
    }

    public String get() {
        if (n >= mirrors.length) {
            reset();
        }
        return mirrors[n++];
    }
}
