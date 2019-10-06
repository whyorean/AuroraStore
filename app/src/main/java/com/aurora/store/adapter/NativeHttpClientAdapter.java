/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
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

package com.aurora.store.adapter;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.aurora.store.exception.AppNotFoundException;
import com.aurora.store.exception.MalformedRequestException;
import com.aurora.store.exception.TooManyRequestsException;
import com.aurora.store.exception.UnknownException;
import com.aurora.store.utility.Util;
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
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class NativeHttpClientAdapter extends HttpClientAdapter {

    static private final int TIMEOUT = 15000;
    private Context context;

    public NativeHttpClientAdapter(Context context) {
        this.context = context;
    }

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
        bufferedInputStream.close();
        outputStream.close();
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
        HttpURLConnection connection;
        if (Util.isNetworkProxyEnabled(context))
            connection = (HttpURLConnection) new URL(buildUrl(url, params)).openConnection(Util.getNetworkProxy(context));
        else
            connection = (HttpURLConnection) new URL(buildUrl(url, params)).openConnection();
        return request(connection, null, headers);
    }

    @Override
    public byte[] getEx(String url, Map<String, List<String>> params, Map<String, String> headers) throws IOException {
        HttpURLConnection connection;
        if (Util.isNetworkProxyEnabled(context))
            connection = (HttpURLConnection) new URL(buildUrlEx(url, params)).openConnection(Util.getNetworkProxy(context));
        else
            connection = (HttpURLConnection) new URL(buildUrlEx(url, params)).openConnection();
        return request(connection, null, headers);
    }

    @Override
    public byte[] postWithoutBody(String url, Map<String, String> urlParams, Map<String, String> headers) throws IOException {
        return post(buildUrl(url, urlParams), new byte[0], headers);
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
        HttpURLConnection connection;
        if (Util.isNetworkProxyEnabled(context))
            connection = (HttpURLConnection) new URL(url).openConnection(Util.getNetworkProxy(context));
        else
            connection = (HttpURLConnection) new URL(url).openConnection();
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

        byte[] content;
        connection.connect();

        int code = 0;

        boolean isGzip;
        try {
            isGzip = null != connection.getContentEncoding() && connection.getContentEncoding().contains("gzip");
        } catch (NullPointerException e) {
            throw new AuthException(e.getMessage(), 401);
        }

        try {
            code = connection.getResponseCode();
            content = readFully(connection.getInputStream(), isGzip);
        } catch (IOException e) {
            content = readFully(connection.getErrorStream(), isGzip);
        } finally {
            connection.disconnect();
        }

        if (code == 401 || code == 403) {
            AuthException authException = new AuthException("Auth error", code);
            Map<String, String> authResponse = GooglePlayAPI.parseResponse(new String(content));
            if (authResponse.containsKey("Error") && authResponse.get("Error").equals("NeedsBrowser")) {
                authException.setTwoFactorUrl(authResponse.get("Url"));
            }
            throw authException;
        } else if (code == 404) {
            Map<String, String> authResponse = GooglePlayAPI.parseResponse(new String(content));
            if (authResponse.containsKey("Error") && authResponse.get("Error").equals("UNKNOWN_ERR")) {
                throw new UnknownException("Unknown error occurred", code);
            } else
                throw new AppNotFoundException("App not found", code);
        } else if (code == 429) {
            throw new TooManyRequestsException("Rate-limiting enabled, you are making too many requests", code);
        } else if (code >= 500) {
            throw new GooglePlayException("Server error", code);
        } else if (code >= 400) {
            throw new MalformedRequestException("Malformed Request", code);
        }
        return content;
    }
}
