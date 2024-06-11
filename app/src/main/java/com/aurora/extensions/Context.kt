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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.util.Log
import com.aurora.store.util.Preferences

val Context.inflater: LayoutInflater
    get() = LayoutInflater.from(this)

fun Context.browse(url: String, showOpenInAuroraAction: Boolean = false) {
    try {
        val customTabsIntent = CustomTabsIntent.Builder()
        if (showOpenInAuroraAction) {
            val icon =
                ContextCompat.getDrawable(this, R.drawable.ic_open_in_new)?.toBitmap()
            val pendingIntent = PendingIntentCompat.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                0,
                false
            )
            customTabsIntent.setActionButton(
                icon!!,
                this.getString(R.string.open_in_aurora),
                pendingIntent!!,
                true
            )
        }
        customTabsIntent.build().launchUrl(this, Uri.parse(url))
    } catch (e: Exception) {
        Log.e(e.message)
    }
}

fun Context.share(app: App) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, app.displayName)
            putExtra(Intent.EXTRA_TEXT, "${Constants.SHARE_URL}${app.packageName}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.action_share)))
    } catch (exception: Exception) {
        Log.e("Failed to share app", exception)
    }
}

fun Context.openInfo(packageName: String) {
    try {
        val intent = Intent(
            "android.settings.APPLICATION_DETAILS_SETTINGS",
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    } catch (e: Exception) {

    }
}

fun <T> Context.open(className: Class<T>, newTask: Boolean = false) {
    val intent = Intent(this, className)
    if (newTask)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(
        intent,
        getEmptyActivityBundle()
    )
}

fun Context.getEmptyActivityBundle(): Bundle? {
    return ActivityOptionsCompat.makeCustomAnimation(
        this,
        android.R.anim.fade_in,
        android.R.anim.fade_out
    ).toBundle()
}

fun Context.copyToClipBoard(data: String?) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Download Url", data)
    clipboard.setPrimaryClip(clip)
}

fun Context.getStyledAttributeColor(id: Int): Int {
    val arr = obtainStyledAttributes(TypedValue().data, intArrayOf(id))
    val styledAttr = arr.getColor(0, Color.WHITE)
    arr.recycle()
    return styledAttr
}

fun Context.accentColor(): Int {
    val color = when (Preferences.getInteger(this, Preferences.PREFERENCE_THEME_ACCENT)) {
        0 -> R.color.colorAccent
        1 -> R.color.colorAccent01
        2 -> R.color.colorAccent02
        3 -> R.color.colorAccent03
        4 -> R.color.colorAccent04
        5 -> R.color.colorAccent05
        6 -> R.color.colorAccent06
        7 -> R.color.colorAccent07
        8 -> R.color.colorAccent08
        9 -> R.color.colorAccent09
        10 -> R.color.colorAccent10
        11 -> R.color.colorAccent11
        12 -> R.color.colorAccent12
        13 -> R.color.colorAccent13
        else -> if (isSAndAbove()) R.color.colorAccent else R.color.colorAccent01
    }
    return ContextCompat.getColor(this, color)
}

fun Context.isIgnoringBatteryOptimizations(): Boolean {
    return if (isMAndAbove()) {
        (getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
    } else {
        true
    }
}
