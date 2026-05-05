package com.aurora.store.data.model

data class ExternalItem(
    val packageName: String,
    val displayName: String,
    val iconURL: String,
    val size: Long,
    val status: InstallStatus,
    val progress: Int = 0,
    val speed: Long = 0L,
    val timeRemaining: Long = -1L
)
