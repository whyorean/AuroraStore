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

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

fun View.isVisible() = visibility == View.VISIBLE

fun View.isGone() = visibility == View.GONE

fun View.isInvisible() = visibility == View.INVISIBLE

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
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    this.requestFocus()
    imm.showSoftInput(this, 0)
}

fun View.hideKeyboard(): Boolean {
    try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.hideSoftInputFromWindow(windowToken, 0)
    } catch (ignored: RuntimeException) {
    }
    return false
}

fun View.setOnSingleClickListener(tolerance: Long = 500, onClick: (v: View) -> Unit) {
    var lastClicked = 0L
    val currentTimeMillis = System.currentTimeMillis()
    setOnClickListener {
        if (currentTimeMillis - lastClicked > tolerance) {
            onClick(it)
            lastClicked = currentTimeMillis
        }
    }
}

fun View.rotate(resetToZero: Boolean = true, duration: Long = 400) {
    if (resetToZero)
        rotation = 0f
    animate().rotation(360f).setDuration(duration).start()
}

fun View.flip(resetToZero: Boolean = true, duration: Long = 400) {
    if (resetToZero)
        rotation = 0f
    animate().rotation(180f).setDuration(duration).start()
}

fun View.getString(resourceId: Int): String {
    return context.getString(resourceId)
}

