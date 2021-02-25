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

import android.text.format.DateFormat
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

fun Long.toDate(): String {
    val calendar = Calendar.getInstance(Locale.getDefault())
    calendar.timeInMillis = this
    return DateFormat.format("dd/MM/yy", calendar).toString()
}

fun Throwable.stackTraceToString(): String {
    val stringWriter = StringWriter(1024)
    val printWriter = PrintWriter(stringWriter)
    printStackTrace(printWriter)
    printWriter.close()
    return stringWriter.toString()
}