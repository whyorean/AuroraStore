/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import android.Manifest
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.aurora.Constants
import com.aurora.store.ComposeActivity
import com.aurora.store.R
import com.aurora.store.compose.navigation.Screen

private const val TAG = "Context"

val Context.inflater: LayoutInflater
    get() = LayoutInflater.from(this)

fun Context.browse(url: String) {
    try {
        val customTabsIntent = CustomTabsIntent.Builder()
        customTabsIntent.build().launchUrl(this, url.toUri())
    } catch (exception: Exception) {
        Log.e(TAG, "Failed to open custom tab", exception)
    }
}

fun Context.viewExternal(url: String): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (_: Exception) {
    Log.e(TAG, "No app to handle $url")
    false
}

fun Context.appInfo(packageName: String) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
        }
        startActivity(intent)
    } catch (exception: Exception) {
        Log.e(TAG, "Failed to open app info", exception)
    }
}

fun Context.share(displayName: String, packageName: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, displayName)
            putExtra(Intent.EXTRA_TEXT, "${Constants.SHARE_URL}$packageName")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.action_share)))
    } catch (exception: Exception) {
        Log.e(TAG, "Failed to share app", exception)
    }
}

fun Context.mailTo(email: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SENDTO
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, email)
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.details_dev_email)))
    } catch (exception: Exception) {
        Log.e(TAG, "Failed to email", exception)
    }
}

fun Context.openInfo(packageName: String) {
    try {
        val intent = Intent(
            "android.settings.APPLICATION_DETAILS_SETTINGS",
            "package:$packageName".toUri()
        )
        startActivity(intent)
    } catch (exception: Exception) {
        Log.e(TAG, "Failed to open app info page", exception)
    }
}

fun <T> Context.open(className: Class<T>, newTask: Boolean = false) {
    val intent = Intent(this, className)
    if (newTask) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    startActivity(
        intent,
        getEmptyActivityBundle()
    )
}

fun Context.getEmptyActivityBundle(): Bundle? = ActivityOptionsCompat.makeCustomAnimation(
    this,
    android.R.anim.fade_in,
    android.R.anim.fade_out
).toBundle()

fun Context.copyToClipBoard(data: String?) {
    val clipboard = getSystemService<ClipboardManager>()
    val clip = ClipData.newPlainText("Download Url", data)
    clipboard?.setPrimaryClip(clip)
}

fun Context.getStyledAttributeColor(id: Int): Int {
    val arr = obtainStyledAttributes(TypedValue().data, intArrayOf(id))
    val styledAttr = arr.getColor(0, Color.WHITE)
    arr.recycle()
    return styledAttr
}

fun Context.isIgnoringBatteryOptimizations(): Boolean =
    getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(packageName) ?: true

fun Context.areNotificationsEnabled(): Boolean = when {
    isNAndAbove -> getSystemService<NotificationManager>()!!.areNotificationsEnabled()
    else -> true
}

fun Context.checkManifestPermission(permission: String): Boolean =
    ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.isExternalStorageAccessible(): Boolean = when {
    isRAndAbove -> Environment.isExternalStorageManager()
    else -> checkManifestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
}

fun Context.isDomainVerified(domain: String): Boolean = when {
    isSAndAbove -> {
        val domainVerificationManager = getSystemService<DomainVerificationManager>()
        val userState = domainVerificationManager!!.getDomainVerificationUserState(packageName)
        val domainMap = userState?.hostToStateMap?.filterKeys { it == domain }
        domainMap?.values?.first() == DomainVerificationUserState.DOMAIN_STATE_SELECTED
    }

    else -> true
}

fun Context.navigate(screen: Screen) {
    val intent = Intent(this, ComposeActivity::class.java).apply {
        putExtra(Screen.PARCEL_KEY, screen)
    }
    startActivity(intent)
}
