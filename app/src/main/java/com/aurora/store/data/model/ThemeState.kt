/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import androidx.annotation.DrawableRes
import com.aurora.store.R

/**
 * Values for supported theme states
 */
enum class ThemeState(@DrawableRes val icon: Int) {
    AUTO(R.drawable.ic_auto),
    LIGHT(R.drawable.ic_light),
    DARK(R.drawable.ic_dark)
}
