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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.dragons.aurora.helpers.Prefs;
import com.percolate.caffeine.PhoneUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import timber.log.Timber;

public class BitmapManager {

    static private final long VALID_MILLIS = 1000 * 60 * 60 * 24 * 7;
    static private LruCache<String, Bitmap> memoryCache;

    static {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new X509TrustManager[]{new NullX509TrustManager()}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (NoSuchAlgorithmException e) {
            // Impossible
        } catch (KeyManagementException e) {
            // Impossible
        }
        final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
            }
        };
    }

    private File baseDir;
    private boolean noImages;

    public BitmapManager(Context context) {
        baseDir = context.getCacheDir();
        noImages = Prefs.getBoolean(context, Aurora.PREFERENCE_NO_IMAGES) && PhoneUtils.isConnectedMobile(context);
    }

    static private boolean isStoredAndValid(File cached) {
        return cached.exists()
                && cached.lastModified() + VALID_MILLIS > System.currentTimeMillis()
                && cached.length() > 0
                ;
    }

    static private Bitmap getCachedBitmapFromDisk(File cached) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inDither = false;
            return BitmapFactory.decodeStream(new FileInputStream(cached), null, options);
        } catch (IOException e) {
            Timber.e("Could not get cached bitmap: " + e.getClass().getName() + " " + e.getMessage());
            return null;
        }
    }

    static private void cacheBitmapOnDisk(Bitmap bitmap, File cached) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(cached);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Util.closeSilently(out);
        }
    }

    static private void cacheBitmapInMemory(String url, Bitmap bitmap) {
        if (null != url && null != bitmap) {
            memoryCache.put(url, bitmap);
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
            Timber.e("Could not get icon from " + url + " " + e.getMessage());
        }
        return null;
    }

    public Bitmap getBitmap(String url, boolean fullSize) {
        Bitmap bitmap = memoryCache.get(url);
        if (null != bitmap) {
            return bitmap;
        }
        File onDisk = getFile(url);
        if (isStoredAndValid(onDisk)) {
            bitmap = getCachedBitmapFromDisk(onDisk);
            cacheBitmapInMemory(url, bitmap);
            return bitmap;
        }
        if (noImages) {
            return null;
        }
        bitmap = downloadBitmap(url, fullSize);
        if (null != bitmap) {
            cacheBitmapOnDisk(bitmap, onDisk);
            cacheBitmapInMemory(url, bitmap);
        }
        return bitmap;
    }

    public File downloadAndGetFile(String url) {
        File onDisk = getFile(url);
        if (isStoredAndValid(onDisk)) {
            return onDisk;
        }
        Bitmap bitmap = downloadBitmap(url, true);
        if (null != bitmap) {
            cacheBitmapOnDisk(bitmap, onDisk);
            return onDisk;
        }
        return null;
    }

    private File getFile(String urlString) {
        return new File(baseDir, String.valueOf(urlString.hashCode()) + ".png");
    }
}
