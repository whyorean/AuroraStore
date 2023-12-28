package com.aurora.store.data.model

data class Request(
    val url: String,
    val filePath: String,
    val size: Long,
    val sha1: String,
    val sha256: String
)
