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
    VERIFYING(R.string.status_verifying),
    PURCHASING(R.string.preparing_to_install);

    companion object {
        val finished = listOf(FAILED, CANCELLED, COMPLETED)
        val running = listOf(QUEUED, PURCHASING, DOWNLOADING)

        /**
         * States in which a download worker is actively occupying the (single) download
         * slot — purchasing, transferring bytes or verifying. Used to serialize downloads:
         * the next [QUEUED] item is only started once none of these are in progress, so
         * concurrent workers can't clobber the shared foreground/progress notification.
         */
        val processing = setOf(PURCHASING, DOWNLOADING, VERIFYING)
    }
}
