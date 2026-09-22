/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import android.content.Context
import android.util.Log
import com.aurora.store.R
import com.aurora.store.data.model.ProxyInfo
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
        if (value <= 1L) {
            return "NA"
        }
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
            Locale.getDefault(),
            "%.1f %sB/s",
            bytes / unit.toDouble().pow(exp.toDouble()),
            pre
        )
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
        val pattern = """^(https?|socks5?)://(?:([^\s:@]+):([^\s:@]+)@)?([^\s:@]+):(\d+)$"""
        val match = pattern.toRegex().find(proxyUrl)

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
}
