package com.jmods.domain.model

data class InstalledAppInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean
)
