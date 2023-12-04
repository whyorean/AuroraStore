package com.aurora.store.data.model

enum class DownloadStatus {
    DOWNLOADING,
    FAILED,
    CANCELLED,
    COMPLETED,
    QUEUED,
    UNAVAILABLE;

    companion object {
        val finished = listOf(FAILED, CANCELLED, COMPLETED)
    }
}
