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

import android.annotation.SuppressLint;
import android.preference.PreferenceManager;

import com.dragons.aurora.SharedPreferencesTranslator;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.playstore.DependencyTranslationTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GoogleDependency extends AbstractHelper {

    private SharedPreferencesTranslator translator;

    public GoogleDependency(DetailsFragment fragment, App app) {
        super(fragment, app);
        translator = new SharedPreferencesTranslator(PreferenceManager.getDefaultSharedPreferences(fragment.getActivity()));
    }

    @Override
    public void draw() {
        Set<String> untranslated = new HashSet<>();
        Set<String> translated = getTranslatedDeps(app);
        for (String dependency : translated) {
            if (app.getDependencies().contains(dependency)) {
                untranslated.add(dependency);
            }
        }

        if (untranslated.size() > 0) {
            getTranslations(untranslated);
        }
    }

    private Set<String> getTranslatedDeps(App app) {
        Set<String> translated = new HashSet<>();
        for (String dependency : app.getDependencies()) {
            translated.add(translator.getString(dependency));
        }
        return translated;
    }

    private void getTranslations(Set<String> untranslated) {
        @SuppressLint("StaticFieldLeak")
        DependencyTranslationTask task = new DependencyTranslationTask() {

            @Override
            protected void onPostExecute(List<App> apps) {
                super.onPostExecute(apps);
                if (!success()) {
                    return;
                }
                for (String packageName : translated.keySet()) {
                    translator.putString(packageName, translated.get(packageName));
                }
            }
        };
        task.setContext(fragment.getActivity());
        task.execute(untranslated.toArray(new String[untranslated.size()]));
    }
}