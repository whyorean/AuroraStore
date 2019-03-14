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

package com.aurora.store.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.aurora.store.utility.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BitmapManager {

    private File baseDir;

    public BitmapManager(Context context) {
        baseDir = context.getCacheDir();
    }

    static private void cacheBitmapOnDisk(Bitmap bitmap, File cached) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(cached);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static private Bitmap downloadBitmap(String url, boolean fullSize) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();
            connection.setConnectTimeout(3000);
            InputStream input = connection.getInputStream();

            BitmapFactory.Options options = new BitmapFactory.Options();
            if (!fullSize) {
                options.inSampleSize = 4;
            }
            options.inJustDecodeBounds = false;

            return BitmapFactory.decodeStream(input, null, options);
        } catch (IOException e) {
            Log.e("Could not get icon from " + url + " " + e.getMessage());
        }
        return null;
    }

    public File downloadAndGetFile(String url) {
        File onDisk = getFile(url);
        Bitmap bitmap = downloadBitmap(url, true);
        if (null != bitmap) {
            cacheBitmapOnDisk(bitmap, onDisk);
            return onDisk;
        }
        return null;
    }

    private File getFile(String urlString) {
        return new File(baseDir, urlString.hashCode() + ".png");
    }
}