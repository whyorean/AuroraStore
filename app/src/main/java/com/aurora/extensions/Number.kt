/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import android.content.res.Resources

val Number.px: Number
    get() = (this.toFloat() * Resources.getSystem().displayMetrics.density).toInt()
