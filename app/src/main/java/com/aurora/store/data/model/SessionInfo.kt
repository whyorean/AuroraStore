package com.aurora.store.data.model

data class SessionInfo(
    val sessionId: Int,
    val packageName: String,
    val versionCode: Long,
    val displayName: String = String()
)
