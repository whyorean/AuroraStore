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
import android.util.Log;

import com.dragons.aurora.model.App;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class DeltaPatcherGDiffGzipped extends DeltaPatcherGDiff {

    public DeltaPatcherGDiffGzipped(Context context, App app) {
        super(context, app);
    }

    static private boolean GUnZip(File from, File to) {
        GZIPInputStream zipInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            zipInputStream = new GZIPInputStream(new FileInputStream(from));
            fileOutputStream = new FileOutputStream(to);
            byte[] buffer = new byte[0x1000];
            int count;
            while ((count = zipInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, count);
            }
            return true;
        } catch (IOException e) {
            Log.e(DeltaPatcherGDiff.class.getSimpleName(), "Could not unzip the patch: " + e.getMessage());
            return false;
        } finally {
            Util.closeSilently(fileOutputStream);
            Util.closeSilently(zipInputStream);
        }
    }

    @Override
    public boolean patch() {
        File patchUncompressed = new File(patch.getAbsolutePath() + ".unpacked");
        Log.i(DeltaPatcherGDiff.class.getSimpleName(), "Decompressing");
        File patchCompressed = patch;
        if (!GUnZip(patchCompressed, patchUncompressed)) {
            return false;
        }
        Log.i(DeltaPatcherGDiff.class.getSimpleName(), "Deleting " + patchCompressed);
        patchCompressed.delete();
        patch = patchUncompressed;
        return super.patch();
    }
}
