package com.aurora.store.data.network

import com.aurora.gplayapi.network.IHttpClient
import java.net.Proxy

interface IProxyHttpClient : IHttpClient {
    @Throws(UnsupportedOperationException::class)
    fun setProxy(proxy: Proxy, proxyUser: String?, proxyPassword: String?): IHttpClient
}