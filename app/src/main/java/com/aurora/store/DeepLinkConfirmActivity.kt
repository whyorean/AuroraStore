/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.compose.theme.AuroraTheme
import com.aurora.store.compose.ui.sheets.DeepLinkConfirmSheet
import com.aurora.store.util.Preferences

/**
 * Translucent trampoline that gates external [Intent.ACTION_VIEW] app/developer listing deep links
 * (market:// and play.google.com links). These are the vector ads exploit to launch Aurora into a
 * listing without intent, so a Play Store-style confirmation sheet is shown floating over the
 * launching app before forwarding to [ComposeActivity]. When the user has opted out, or the intent
 * doesn't resolve to a listing, it forwards immediately without prompting.
 */
class DeepLinkConfirmActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val target = resolveDeepLink()
        val shouldConfirm = target != null &&
            Preferences.getBoolean(this, Preferences.PREFERENCE_CONFIRM_EXTERNAL_DEEPLINK, true)

        if (!shouldConfirm) {
            forwardToAurora(target)
            return
        }

        val referrerLabel = resolveReferrerLabel()
        setContent {
            AuroraTheme {
                DeepLinkConfirmSheet(
                    targetLabel = target.deepLinkLabel(),
                    sourceLabel = referrerLabel,
                    onOpen = { forwardToAurora(target) },
                    onDismiss = { finish() }
                )
            }
        }
    }

    /**
     * Resolves the listing requested by the incoming ACTION_VIEW intent, or null when the intent
     * carries no id. The action keyword ("details", "dev" or "developer") is the last path segment
     * for play.google.com links and the host for market:// links. Both "dev" and "developer" links
     * may carry either a numeric developer id (curated developer stream) or a developer name
     * (publisher search), so the id is parsed to decide which one to open.
     */
    private fun resolveDeepLink(): Screen? {
        if (intent.action != Intent.ACTION_VIEW) return null

        val data = intent.data ?: return null
        val id = data.getQueryParameter("id") ?: return null
        return when (data.lastPathSegment ?: data.host) {
            "dev", "developer" -> when {
                id.toLongOrNull() != null -> Screen.DevProfile(id)
                else -> Screen.PublisherProfile(id)
            }

            else -> Screen.AppDetails(id)
        }
    }

    /**
     * Best-effort human-readable name of the app that fired the intent, derived from the activity
     * referrer. Resolves an android-app:// referrer to its app label, falling back to the raw host.
     * Returns null when no referrer is available.
     */
    private fun resolveReferrerLabel(): String? {
        val ref = referrer ?: return null
        val pkg = if (ref.scheme == "android-app") ref.host else null
        if (pkg != null) {
            return runCatching {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(pkg, 0)
                ).toString()
            }.getOrDefault(pkg)
        }
        return ref.host ?: ref.toString()
    }

    private fun forwardToAurora(target: Screen?) {
        startActivity(
            Intent(this, ComposeActivity::class.java).apply {
                target?.let { putExtra(Screen.PARCEL_KEY, it) }
                // Start ComposeActivity fresh so the parcel is honoured even when Aurora is already
                // running; without this a reused instance keeps its current screen. Mirrors the
                // deep-link PendingIntents in NotificationUtil.
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun Screen.deepLinkLabel(): String = when (this) {
        is Screen.AppDetails -> packageName
        is Screen.DevProfile -> developerId
        is Screen.PublisherProfile -> publisherId
        else -> ""
    }
}
