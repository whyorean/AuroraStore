package com.aurora.store.data.network

import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.data.model.ProxyInfo
import java.net.Proxy

interface IProxyHttpClient : IHttpClient {
    @Throws(UnsupportedOperationException::class)
    fun setProxy(proxyInfo: ProxyInfo): IHttpClient
}