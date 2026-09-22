/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Context.toast(resId: Int) = runOnUiThread {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).apply { show() }
}

fun Context.toast(text: CharSequence) = runOnUiThread {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).apply { show() }
}

fun Fragment.toast(resId: Int) = requireContext().toast(resId)

fun Fragment.toast(text: CharSequence) = requireContext().toast(text)
