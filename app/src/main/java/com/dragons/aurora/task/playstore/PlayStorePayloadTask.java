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

import android.content.Context;

import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.IteratorGooglePlayException;

import java.io.IOException;

abstract public class PlayStorePayloadTask<T> extends PlayStoreTask<T> {

    abstract protected T getResult(GooglePlayAPI api, String... arguments) throws IOException;

    @Override
    public void setContext(Context context) {
        super.setContext(context);
    }

    @Override
    protected T doInBackground(String... arguments) {
        try {
            return getResult(new PlayStoreApiAuthenticator(context).getApi(), arguments);
        } catch (IOException e) {
            exception = e;
        } catch (IteratorGooglePlayException e) {
            exception = e.getCause();
        }
        return null;
    }
}
