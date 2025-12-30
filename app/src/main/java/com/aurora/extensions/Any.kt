/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

/**
 * Simple extension method to provide logging TAG
 */
inline val <reified T> T.TAG: String
    get() = when {
        T::class.java.isAnonymousClass -> T::class.java.name
        else -> T::class.java.simpleName
    }
