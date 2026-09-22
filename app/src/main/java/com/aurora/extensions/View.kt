/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService

fun View.isVisible() = visibility == View.VISIBLE

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.showKeyboard() {
    val imm = context.getSystemService<InputMethodManager>()
    this.requestFocus()
    imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.hideKeyboard(): Boolean {
    try {
        val imm = context.getSystemService<InputMethodManager>()
        return imm?.hideSoftInputFromWindow(windowToken, 0) ?: false
    } catch (ignored: RuntimeException) {
    }
    return false
}
