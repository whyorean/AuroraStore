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

import android.graphics.Color
import android.text.format.DateFormat
import androidx.annotation.ColorInt
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Calendar
import java.util.Locale
import javax.annotation.Nullable

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

fun isValidPackageName(packageName: String): Boolean {
    val packageRegex = "^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*$".toRegex()
    return packageName.matches(packageRegex)
}

/**
 * Computes a darker color from the given color.
 * @param color The color to darken.
 * @param factor The factor to darken the color by.
 *  - higher factor values will result in a lighter color.
 * @return The darker color.
 */
fun darkenColor(@ColorInt color: Int, factor: Float = 0.25f): Int {
    val a = Color.alpha(color)
    val r = (Color.red(color) * factor).coerceIn(0f, 255f).toInt()
    val g = (Color.green(color) * factor).coerceIn(0f, 255f).toInt()
    val b = (Color.blue(color) * factor).coerceIn(0f, 255f).toInt()

    return Color.argb(a, r, g, b)
}

/**
 * Computes a lighter color from the given color.
 * @param color The color to lighten.
 * @param factor The factor to lighten the color by.
 *  - higher factor values will result in a lighter color.
 * @param alpha The alpha value to use for the lighter color.
 * @return The lighter color.
 */
fun lightenColor(@ColorInt color: Int, factor: Float = 0.5f, @Nullable alpha: Int? = null): Int {
    val a = alpha ?: Color.alpha(color)
    val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt()
    val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt()
    val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt()

    return Color.argb(a, r, g, b)
}

/**
 * Computes a contrasting color from the given color.
 * @param color The color to contrast.
 * @return The contrasting color.
 */
fun contrastingColor(@ColorInt color: Int): Int {
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)
    val yiq = ((red * 299) + (green * 587) + (blue * 114)) / 1000

    return if (yiq >= 128) Color.BLACK else Color.WHITE
}