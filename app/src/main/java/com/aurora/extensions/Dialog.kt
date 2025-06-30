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
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Context.showDialog(@StringRes titleId: Int, @StringRes messageId: Int) {
    showDialog(getString(titleId), getString(messageId), null, null)
}

fun Context.showDialog(title: String?, message: String?) {
    showDialog(title, message, null, null)
}

fun Context.showDialog(
    title: String?,
    message: String?,
    positiveListener: DialogInterface.OnClickListener?,
    negativeListener: DialogInterface.OnClickListener?
) {
    runOnUiThread {
        val builder = MaterialAlertDialogBuilder(this).apply {
            setTitle(title)
            setMessage(message)

            if (positiveListener != null) {
                setPositiveButton(android.R.string.ok, positiveListener)
            } else {
                setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            }

            negativeListener?.let {
                setNegativeButton(android.R.string.cancel, negativeListener)
            }

        }.create()

        builder.show()
    }
}

fun Fragment.showDialog(@StringRes titleId: Int, @StringRes messageId: Int) {
    requireContext().showDialog(titleId, messageId)
}
