package com.aurora.next.domain.model

data class App(
    val id: String,
    val name: String,
    val packageName: String,
    val description: String,
    val iconUrl: String,
    val version: String,
    val size: Long,
    val isInstalled: Boolean = false,
    val hasUpdate: Boolean = false
)
