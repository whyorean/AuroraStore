/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
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
