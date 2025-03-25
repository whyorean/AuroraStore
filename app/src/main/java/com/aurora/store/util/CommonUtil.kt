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

package com.aurora.store.util

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.aurora.store.R
import com.aurora.store.data.model.ProxyInfo
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object CommonUtil {

    private const val TAG = "CommonUtil"

    private val siPrefixes: Map<Int, String> = hashMapOf(
        Pair(0, ""),
        Pair(1, ""),
        Pair(3, " KB"),
        Pair(6, " MB"),
        Pair(9, " GB")
    )

    private val diPrefixes: Map<Int, String> = hashMapOf(
        Pair(0, ""),
        Pair(1, ""),
        Pair(3, " K"),
        Pair(6, " M"),
        Pair(9, " B")
    )

    fun addSiPrefix(value: Long): String {
        if (value <= 1L)
            return "NA"
        var tempValue = value
        var order = 0
        while (tempValue >= 1000.0) {
            tempValue /= 1000.toLong()
            order += 3
        }
        return tempValue.toString() + siPrefixes[order]
    }

    fun addDiPrefix(value: Long): String? {
        if (value <= 1L) return null
        var tempValue = value
        var order = 0
        while (tempValue >= 1000.0) {
            tempValue /= 1000.0.toLong()
            order += 3
        }
        return tempValue.toString() + diPrefixes[order]
    }

    fun getETAString(context: Context, etaInMilliSeconds: Long): String {
        if (etaInMilliSeconds < 0) {
            return context.getString(R.string.download_eta_calculating)
        }
        var seconds = (etaInMilliSeconds / 1000).toInt()
        val hours = (seconds / 3600).toLong()
        seconds -= (hours * 3600).toInt()
        val minutes = (seconds / 60).toLong()
        seconds -= (minutes * 60).toInt()
        return when {
            hours > 0 -> {
                context.getString(R.string.download_eta_hrs, hours, minutes, seconds)
            }

            minutes > 0 -> {
                context.getString(R.string.download_eta_min, minutes, seconds)
            }

            else -> {
                context.getString(R.string.download_eta_sec, seconds)
            }
        }
    }

    fun humanReadableByteSpeed(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1].toString() + if (si) "" else "i"
        return String.format(
            Locale.getDefault(), "%.1f %sB/s",
            bytes / unit.toDouble().pow(exp.toDouble()),
            pre
        )
    }

    fun humanReadableByteValue(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1].toString() + if (si) "" else "i"
        return String.format(
            Locale.getDefault(), "%.1f %sB",
            bytes / unit.toDouble().pow(exp.toDouble()),
            pre
        )
    }

    fun getDownloadSpeedString(context: Context, downloadedBytesPerSecond: Long): String {
        if (downloadedBytesPerSecond < 0) {
            return context.getString(R.string.download_speed_estimating)
        }
        val kb = downloadedBytesPerSecond.toDouble() / 1000.toDouble()
        val mb = kb / 1000.toDouble()
        val decimalFormat = DecimalFormat(".##")
        return when {
            mb >= 1 -> {
                context.getString(R.string.download_speed_mb, decimalFormat.format(mb))
            }

            kb >= 1 -> {
                context.getString(R.string.download_speed_kb, decimalFormat.format(kb))
            }

            else -> {
                context.getString(R.string.download_speed_bytes, downloadedBytesPerSecond)
            }
        }
    }

    fun cleanupInstallationSessions(context: Context) {
        val packageInstaller = context.packageManager.packageInstaller
        for (sessionInfo in packageInstaller.mySessions) {
            try {
                val sessionId = sessionInfo.sessionId
                packageInstaller.abandonSession(sessionInfo.sessionId)
                Log.i(TAG, "Abandoned session id -> $sessionId")
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to cleanup installation sessions")
            }
        }
    }

    fun parseProxyUrl(proxyUrl: String): ProxyInfo? {
        val pattern = """^(https?|socks5?)://(?:([^\s:@]+):([^\s:@]+)@)?([^\s:@]+):(\d+)$""".toRegex()
        val match = pattern.find(proxyUrl)

        return when {
            match != null -> {
                val protocol = match.groupValues[1].uppercase()
                val username = match.groupValues[2]
                val password = match.groupValues[3]
                val url = match.groupValues[4]
                val port = match.groupValues[5]

                ProxyInfo(
                    protocol,
                    url,
                    port.toInt(),
                    username,
                    password
                )
            }

            else -> null
        }
    }

    fun inForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
    }
}
