/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import android.content.Context
import com.jakewharton.processphoenix.ProcessPhoenix

object RestartUtil {
    /**
     * Restarts the whole process so all Hilt @Singletons (incl. the gplayapi helpers, which are
     * bound to the previous default account's AuthData at construction) are rebuilt against the
     * newly selected default account. Uses ProcessPhoenix, matching the app's other restart sites.
     */
    fun restart(context: Context) {
        ProcessPhoenix.triggerRebirth(context)
    }
}
