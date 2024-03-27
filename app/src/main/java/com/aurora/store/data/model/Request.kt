package com.aurora.store.data.model

import java.io.File

data class Request(
    val url: String,
    val file: File,
    val size: Long,
    val sha1: String,
    val sha256: String
)
