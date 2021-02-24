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
import android.widget.Toast
import androidx.annotation.StringRes

fun Context.toast(text: CharSequence): Toast =
    Toast.makeText(this, text, Toast.LENGTH_SHORT).apply { show() }

fun Context.longToast(text: CharSequence): Toast =
    Toast.makeText(this, text, Toast.LENGTH_LONG).apply { show() }

fun Context.toast(@StringRes resId: Int): Toast =
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).apply { show() }

fun Context.longToast(@StringRes resId: Int): Toast =
    Toast.makeText(this, resId, Toast.LENGTH_LONG).apply { show() }
