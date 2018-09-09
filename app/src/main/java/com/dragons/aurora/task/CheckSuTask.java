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

package com.dragons.aurora.task;

import android.os.AsyncTask;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.PreferenceFragment;

import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import eu.chainfire.libsuperuser.Shell;

public class CheckSuTask extends AsyncTask<Void, Void, Void> {

    protected PreferenceFragment fragment;
    protected boolean available;

    public CheckSuTask(PreferenceFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (!available) {
            ((CheckBoxPreference) fragment.findPreference(Aurora.PREFERENCE_BACKGROUND_UPDATE_INSTALL)).setChecked(false);
            ((ListPreference) fragment.findPreference(Aurora.PREFERENCE_INSTALLATION_METHOD)).setValueIndex(0);
            ContextUtil.toast(fragment.getActivity().getApplicationContext(), R.string.pref_no_root);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        available = Shell.SU.available();
        return null;
    }
}
