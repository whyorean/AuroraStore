package com.aurora.store.data.model

import androidx.annotation.StringRes
import com.aurora.store.R

enum class DownloadStatus(@StringRes val localized: Int) {
    DOWNLOADING(R.string.status_downloading),
    FAILED(R.string.status_failed),
    CANCELLED(R.string.status_cancelled),
    COMPLETED(R.string.status_completed),
    QUEUED(R.string.status_queued),
    UNAVAILABLE(R.string.status_unavailable),
    VERIFYING(R.string.status_verifying);

    companion object {
        val finished = listOf(FAILED, CANCELLED, COMPLETED)
        val running = listOf(QUEUED, DOWNLOADING)
    }
}
