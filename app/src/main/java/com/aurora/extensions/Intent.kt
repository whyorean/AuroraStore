/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import android.content.Intent
import android.net.UrlQuerySanitizer
import android.os.Bundle

fun Intent.getPackageName(fallbackBundle: Bundle? = null): String? = when (action) {
    Intent.ACTION_VIEW -> {
        data?.getQueryParameter("id")
    }

    Intent.ACTION_SEND -> {
        val clipData = getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        UrlQuerySanitizer(clipData).getValue("id")
    }

    Intent.ACTION_SHOW_APP_INFO -> {
        extras?.getString(Intent.EXTRA_PACKAGE_NAME)
    }

    else -> {
        extras?.getString("packageName") ?: fallbackBundle?.getString("packageName")
    }
}
