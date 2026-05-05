/*
 * SPDX-FileCopyrightText: 2025 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import androidx.annotation.StringRes
import com.aurora.store.R

enum class InstallStatus(@StringRes val localized: Int) {
    PENDING(R.string.action_pending),
    DOWNLOADING(R.string.status_downloading),
    INSTALLING(R.string.action_installing),
    INSTALLED(R.string.title_installed),
    FAILED(R.string.status_failed)
}
