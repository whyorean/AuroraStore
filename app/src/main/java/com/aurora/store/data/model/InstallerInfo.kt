/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import androidx.annotation.StringRes

/**
 * Class holding information on a supported installer
 */
data class InstallerInfo(
    val id: Int,
    val installer: Installer,
    val installerPackageNames: List<String>,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    @StringRes val description: Int,
    /** Label of the app providing this installer, where more than one app can serve it. */
    val provider: String? = null
) {
    override fun equals(other: Any?): Boolean = when (other) {
        is InstallerInfo -> other.id == id
        else -> false
    }

    override fun hashCode(): Int = id.hashCode()
}
