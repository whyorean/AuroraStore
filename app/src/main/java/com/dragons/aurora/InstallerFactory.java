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

import com.dragons.aurora.helpers.Prefs;

public class InstallerFactory {

    static public InstallerAbstract get(Context context) {
        String userChoice = Prefs.getString(context, Aurora.PREFERENCE_INSTALLATION_METHOD);
        switch (userChoice) {
            case Aurora.INSTALLATION_METHOD_AURORA:
                return new InstallerAurora(context);
            case Aurora.INSTALLATION_METHOD_PRIVILEGED:
                return new InstallerPrivileged(context);
            case Aurora.INSTALLATION_METHOD_ROOT:
                return new InstallerRoot(context);
            case Aurora.INSTALLATION_METHOD_DEFAULT:
            default:
                return new InstallerDefault(context);
        }
    }
}
