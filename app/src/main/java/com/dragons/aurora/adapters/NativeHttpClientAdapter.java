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

package com.dragons.aurora.adapters;

import android.net.Uri;
import android.text.TextUtils;

import com.dragons.aurora.Util;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.GooglePlayException;
import com.dragons.aurora.playstoreapiv2.HttpClientAdapter;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import timber.log.Timber;

public class NativeHttpClientAdapter extends HttpClientAdapter {

    static private final int TIMEOUT = 15000;

    static private String urlEncode(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Unlikely
        }
        return null;
    }

    static private void addBody(HttpURLConnection connection, byte[] body) throws IOException {
        if (null == body) {
            body = new byte[0];
        }
        connection.addRequestProperty("Content-Length", Integer.toString(body.length));
        if (body.length > 0) {
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(body);
            outputStream.close();
        } else {
            connection.setDoOutput(false);
        }
    }

    static private void processHttpErrorCode(int code, byte[] content) throws GooglePlayException {
        if (code == 401 || code == 403) {
            AuthException e = new AuthException("Auth error", code);
            Map<String, String> authResponse = GooglePlayAPI.parseResponse(new String(content));
            if (authResponse.containsKey("Error") && authResponse.get("Error").equals("NeedsBrowser")) {
                e.setTwoFactorUrl(authResponse.get("Url"));
            }
            throw e;
        } else if (code >= 500) {
            throw new GooglePlayException("Server error", code);
        } else if (code >= 400) {
            throw new GooglePlayException("Malformed request", code);
        }
    }

    static private byte[] readFully(InputStream inputStream, boolean gzipped) throws IOException {
        if (null == inputStream) {
            return new byte[0];
        }
        if (gzipped) {
            inputStream = new GZIPInputStream(inputStream);
        }
        InputStream bufferedInputStream = new BufferedInputStream(inputStream);
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        byte[] result = outputStream.toByteArray();
        Util.closeSilently(bufferedInputStream);
        Util.closeSilently(outputStream);
        return result;
    }

    static private String buildFormBody(Map<String, String> params) {
        List<String> keyValuePairs = new ArrayList<>();
        for (String key : params.keySet()) {
            keyValuePairs.add(urlEncode(key) + "=" + urlEncode(params.get(key)));
        }
        return TextUtils.join("&", keyValuePairs);
    }

    @Override
    public byte[] get(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(buildUrl(url, params)).openConnection();
        return request(connection, null, headers);
    }

    @Override
    public byte[] getEx(String url, Map<String, List<String>> params, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(buildUrlEx(url, params)).openConnection();
        return request(connection, null, headers);
    }

    @Override
    public byte[] postWithoutBody(String url, Map<String, String> urlParams, Map<String, String> headers) throws IOException {
        return post(buildUrl(url, urlParams), new HashMap<>(), headers);
    }

    @Override
    public byte[] post(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        return post(url, buildFormBody(params).getBytes(), headers);
    }

    @Override
    public byte[] post(String url, byte[] body, Map<String, String> headers) throws IOException {
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "application/x-protobuf");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        return request(connection, body, headers);
    }

    @Override
    public String buildUrl(String url, Map<String, String> params) {
        Uri.Builder builder = Uri.parse(url).buildUpon();
        for (String key : params.keySet()) {
            builder.appendQueryParameter(key, params.get(key));
        }
        return builder.build().toString();
    }

    @Override
    public String buildUrlEx(String url, Map<String, List<String>> params) {
        Uri.Builder builder = Uri.parse(url).buildUpon();
        for (String key : params.keySet()) {
            for (String value : params.get(key)) {
                builder.appendQueryParameter(key, value);
            }
        }
        return builder.build().toString();
    }

    protected byte[] request(HttpURLConnection connection, byte[] body, Map<String, String> headers) throws IOException {
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.addRequestProperty("Cache-Control", "max-age=300");
        for (String headerName : headers.keySet()) {
            connection.addRequestProperty(headerName, headers.get(headerName));
        }
        addBody(connection, body);

        byte[] content = new byte[0];
        Timber.i("Requesting %s", connection.getURL().toString());
        connection.connect();

        int code = 0;
        boolean isGzip;
        try {
            isGzip = null != connection.getContentEncoding() && connection.getContentEncoding().contains("gzip");
        } catch (NullPointerException e) {
            // Happens on api<=8 only, see https://issuetracker.google.com/issues/36926705
            // The solution is stop using HttpURLConnection entirely...
            // Luckily, it seems to happen when the token gets stale,
            // which means it can be fixed by redoing the request with a new token
            Timber.e("Buggy HttpURLConnection implementation detected");
            throw new AuthException("Actually this is a NullPointerException thrown by a buggy implementation of HttpURLConnection", 401);
        }
        try {
            code = connection.getResponseCode();
            Timber.i("HTTP result code %s", code);
            content = readFully(connection.getInputStream(), isGzip);
        } catch (IOException e) {
            content = readFully(connection.getErrorStream(), isGzip);
            Timber.e("IOException " + e.getClass().getName() + " " + e.getMessage());
            if (code < 400) {
                throw e;
            }
        } catch (Throwable e) {
            Timber.e("Unknown exception " + e.getClass().getName() + " " + e.getMessage());
        } finally {
            connection.disconnect();
        }
        processHttpErrorCode(code, content);
        return content;
    }
}
