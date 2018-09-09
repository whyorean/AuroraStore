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

import com.dragons.aurora.model.App;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.GZIPInputStream;

import timber.log.Timber;

public class DeltaPatcherFactory {

    static public DeltaPatcherAbstract get(Context context, App app) {
        File patch = Paths.getDeltaPath(context, app.getPackageName(), app.getVersionCode());
        if (isGZipped(patch)) {
            return new DeltaPatcherGDiffGzipped(context, app);
        } else {
            return new DeltaPatcherGDiff(context, app);
        }
    }

    static private boolean isGZipped(File f) {
        int magic = 0;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "r");
            magic = raf.read() & 0xff | ((raf.read() << 8) & 0xff00);
        } catch (IOException e) {
            Timber.e("Could not check if patch is gzipped");
        } finally {
            Util.closeSilently(raf);
        }
        return magic == GZIPInputStream.GZIP_MAGIC;
    }
}
