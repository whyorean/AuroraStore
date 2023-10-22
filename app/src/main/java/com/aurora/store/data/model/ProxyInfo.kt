package com.aurora.store.data.model

data class ProxyInfo(
    var protocol: String,
    var host: String,
    var port: Int,
    var proxyUser: String?,
    var proxyPassword: String?
)
