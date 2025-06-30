/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.extensions

import android.content.Intent
import android.net.UrlQuerySanitizer
import android.os.Bundle

fun Intent.getPackageName(fallbackBundle: Bundle? = null): String? {
    return when (action) {
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
}

