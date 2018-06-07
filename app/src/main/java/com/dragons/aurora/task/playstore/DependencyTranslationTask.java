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

package com.dragons.aurora.task.playstore;

import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependencyTranslationTask extends RemoteAppListTask {

    protected Map<String, String> translated = new HashMap<>();

    @Override
    protected List<App> getResult(GooglePlayAPI api, String... packageNames) throws IOException {
        List<App> result = super.getResult(api, packageNames);
        for (App app : result) {
            translated.put(app.getPackageName(), app.getDisplayName());
        }
        return result;
    }
}
