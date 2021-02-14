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

package com.aurora.store.util.extensions

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.util.Log
import com.aurora.store.util.ViewUtil
import kotlin.system.exitProcess


fun Context.browse(url: String) {
    try {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    } catch (e: Exception) {
        Log.e(e.message)
    }
}

fun Context.share(app: App) {
    try {
        ShareCompat.IntentBuilder.from(this as AppCompatActivity)
            .setType("text/plain")
            .setChooserTitle(getString(R.string.action_share))
            .setSubject(app.displayName)
            .setText(Constants.SHARE_URL + app.packageName)
            .startChooser()
    } catch (e: Exception) {

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
        ViewUtil.getEmptyActivityBundle(this)
    )
}

fun AppCompatActivity.close() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        finishAfterTransition()
    } else {
        finish()
    }
}

fun Context.restartApp() {
    val pendingIntent = PendingIntent.getActivity(
        this,
        1337,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_CANCEL_CURRENT
    )

    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)
    exitProcess(0)
}

fun Context.copyToClipBoard(data: String?) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Download Url", data)
    clipboard.setPrimaryClip(clip)
}
