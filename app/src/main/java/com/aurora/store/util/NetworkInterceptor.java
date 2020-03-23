package com.aurora.store.util;

/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

import com.facebook.stetho.inspector.network.DefaultResponseHandler;
import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl;
import com.facebook.stetho.inspector.network.RequestBodyHelper;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nullable;

import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class NetworkInterceptor implements Interceptor {

    private final NetworkEventReporter networkEventReporter = NetworkEventReporterImpl.get();

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        String requestId = networkEventReporter.nextRequestId();
        Request request = chain.request();
        RequestBodyHelper requestBodyHelper = null;
        if (networkEventReporter.isEnabled()) {
            requestBodyHelper = new RequestBodyHelper(networkEventReporter, requestId);
            OkHttpInspectorRequest inspectorRequest =
                    new OkHttpInspectorRequest(requestId, request, requestBodyHelper);
            networkEventReporter.requestWillBeSent(inspectorRequest);
        }

        Response response;
        try {
            response = chain.proceed(request);
        } catch (IOException e) {
            if (networkEventReporter.isEnabled()) {
                networkEventReporter.httpExchangeFailed(requestId, e.toString());
            }
            throw e;
        }

        if (networkEventReporter.isEnabled()) {
            if (requestBodyHelper != null && requestBodyHelper.hasBody()) {
                requestBodyHelper.reportDataSent();
            }

            Connection connection = chain.connection();
            if (connection == null) {
                throw new IllegalStateException(
                        "No connection associated with this request; " +
                                "did you use addInterceptor instead of addNetworkInterceptor?");
            }
            networkEventReporter.responseHeadersReceived(
                    new OkHttpInspectorResponse(
                            requestId,
                            request,
                            response,
                            connection));

            ResponseBody responseBody = response.body();
            MediaType contentType = null;
            InputStream responseStream = null;
            if (responseBody != null) {
                contentType = responseBody.contentType();
                responseStream = responseBody.byteStream();
            }

            responseStream = networkEventReporter.interpretResponseStream(
                    requestId,
                    contentType != null ? contentType.toString() : null,
                    response.header("Content-Encoding"),
                    responseStream,
                    new DefaultResponseHandler(networkEventReporter, requestId));
            if (responseStream != null) {
                response = response.newBuilder()
                        .body(new ForwardingResponseBody(responseBody, responseStream))
                        .build();
            }
        }
        return response;
    }

    private static class OkHttpInspectorRequest implements NetworkEventReporter.InspectorRequest {
        private final String requestId;
        private final Request request;
        private RequestBodyHelper requestBodyHelper;

        OkHttpInspectorRequest(
                String requestId,
                Request request,
                RequestBodyHelper requestBodyHelper) {
            this.requestId = requestId;
            this.request = request;
            this.requestBodyHelper = requestBodyHelper;
        }

        @Override
        public String id() {
            return requestId;
        }

        @Override
        public String friendlyName() {
            return "Aurora Network Inspector";
        }

        @Nullable
        @Override
        public Integer friendlyNameExtra() {
            return null;
        }

        @Override
        public String url() {
            return request.url().toString();
        }

        @Override
        public String method() {
            return request.method();
        }

        @Nullable
        @Override
        public byte[] body() throws IOException {
            RequestBody body = request.body();
            if (body == null) {
                return null;
            }
            OutputStream out = requestBodyHelper.createBodySink(firstHeaderValue("Content-Encoding"));
            try (BufferedSink bufferedSink = Okio.buffer(Okio.sink(out))) {
                body.writeTo(bufferedSink);
            }
            return requestBodyHelper.getDisplayBody();
        }

        @Override
        public int headerCount() {
            return request.headers().size();
        }

        @Override
        public String headerName(int index) {
            return request.headers().name(index);
        }

        @Override
        public String headerValue(int index) {
            return request.headers().value(index);
        }

        @Nullable
        @Override
        public String firstHeaderValue(String name) {
            return request.header(name);
        }
    }

    private static class OkHttpInspectorResponse implements NetworkEventReporter.InspectorResponse {
        private final String requestId;
        private final Request request;
        private final Response response;
        private final Connection connection;

        OkHttpInspectorResponse(
                String requestId,
                Request request,
                Response response,
                Connection connection) {
            this.requestId = requestId;
            this.request = request;
            this.response = response;
            this.connection = connection;
        }

        @Override
        public String requestId() {
            return requestId;
        }

        @Override
        public String url() {
            return request.url().toString();
        }

        @Override
        public int statusCode() {
            return response.code();
        }

        @Override
        public String reasonPhrase() {
            return response.message();
        }

        @Override
        public boolean connectionReused() {
            return false;
        }

        @Override
        public int connectionId() {
            return connection.hashCode();
        }

        @Override
        public boolean fromDiskCache() {
            return response.cacheResponse() != null;
        }

        @Override
        public int headerCount() {
            return response.headers().size();
        }

        @Override
        public String headerName(int index) {
            return response.headers().name(index);
        }

        @Override
        public String headerValue(int index) {
            return response.headers().value(index);
        }

        @Nullable
        @Override
        public String firstHeaderValue(String name) {
            return response.header(name);
        }
    }

    private static class ForwardingResponseBody extends ResponseBody {
        private final ResponseBody responseBody;
        private final BufferedSource bufferedSource;

        ForwardingResponseBody(ResponseBody body, InputStream interceptedStream) {
            responseBody = body;
            bufferedSource = Okio.buffer(Okio.source(interceptedStream));
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @NotNull
        @Override
        public BufferedSource source() {
            return bufferedSource;
        }
    }
}

