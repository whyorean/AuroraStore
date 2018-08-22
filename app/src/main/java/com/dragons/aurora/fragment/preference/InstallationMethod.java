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

package com.dragons.aurora.fragment.preference;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.dragons.aurora.fragment.PreferenceFragment;

public class InstallationMethod extends Abstract {

    private ListPreference installationMethod;

    public InstallationMethod(PreferenceFragment activity) {
        super(activity);
    }

    public void setInstallationMethodPreference(ListPreference installationMethod) {
        this.installationMethod = installationMethod;
    }

    @Override
    public void draw() {
        Preference.OnPreferenceChangeListener listener = new OnInstallationMethodChangeListener(activity);
        listener.onPreferenceChange(installationMethod, installationMethod.getValue());
        installationMethod.setOnPreferenceChangeListener(listener);
    }
}
