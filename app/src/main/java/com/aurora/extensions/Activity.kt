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

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


fun AppCompatActivity.close() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        finishAfterTransition()
    } else {
        finish()
    }
}

fun AppCompatActivity.restart() {
    val intent = intent
    overridePendingTransition(0, 0)
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
    finish()
    overridePendingTransition(0, 0)
    startActivity(intent)
}

inline fun <reified T : Activity> Activity.startActivity(): Unit =
    this.startActivity(newIntent<T>())

inline fun <reified T : Activity> Activity.startActivity(flags: Int): Unit =
    this.startActivity(newIntent<T>(flags))

inline fun <reified T : Activity> Activity.startActivity(extras: Bundle): Unit =
    this.startActivity(newIntent<T>(extras))

inline fun <reified T : Activity> Activity.startActivity(flags: Int, extras: Bundle): Unit =
    this.startActivity(newIntent<T>(flags, extras))

inline fun <reified T : Activity> Activity.startActivityForResult(requestCode: Int): Unit =
    this.startActivityForResult(newIntent<T>(), requestCode)

inline fun <reified T : Activity> Activity.startActivityForResult(
    requestCode: Int,
    flags: Int
): Unit =
    this.startActivityForResult(newIntent<T>(flags), requestCode)

inline fun <reified T : Activity> Activity.startActivityForResult(
    extras: Bundle, requestCode: Int
): Unit =
    this.startActivityForResult(newIntent<T>(extras), requestCode)

inline fun <reified T : Activity> Activity.startActivityForResult(
    extras: Bundle, requestCode: Int, flags: Int
): Unit =
    this.startActivityForResult(newIntent<T>(flags, extras), requestCode)
