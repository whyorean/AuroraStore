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
import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Context.showDialog(@StringRes titleId: Int, @StringRes messageId: Int) {
    runOnUiThread {
        val backgroundColor: Int = getStyledAttributeColor(android.R.attr.colorBackground)
        val builder = MaterialAlertDialogBuilder(this).apply {
            setTitle(titleId)
            setMessage(messageId)
            setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _ -> dialog.dismiss() }
            background = ColorDrawable(backgroundColor)
        }.create()

        builder.show()
    }
}

fun Context.showDialog(title: String, message: String) {
    runOnUiThread {
        val backgroundColor: Int = getStyledAttributeColor(android.R.attr.colorBackground)
        val builder = MaterialAlertDialogBuilder(this).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _ -> dialog.dismiss() }
            background = ColorDrawable(backgroundColor)
        }.create()

        builder.show()
    }
}

fun Fragment.showDialog(@StringRes titleId: Int, @StringRes messageId: Int) {
    requireContext().showDialog(titleId, messageId)
}

fun Fragment.showDialog(title: String, message: String) {
    requireContext().showDialog(title, message)
}