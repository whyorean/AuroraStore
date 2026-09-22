/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora

object Constants {

    const val PARCEL_DOWNLOAD = "PARCEL_DOWNLOAD"

    const val URL_TOS = "https://play.google.com/about/play-terms/"
    const val URL_LICENSE = "https://gitlab.com/AuroraOSS/AuroraStore/-/tree/master/LICENSES"
    const val URL_DISCLAIMER = "https://gitlab.com/AuroraOSS/AuroraStore/blob/master/DISCLAIMER.md"
    const val URL_POLICY = "https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/POLICY.md"

    const val EXODUS_SUBMIT_PAGE = "https://reports.exodus-privacy.eu.org/analysis/submit/#"
    const val EXODUS_REPORT_URL = "https://reports.exodus-privacy.eu.org/reports/"
    const val EXODUS_SEARCH_URL = "https://reports.exodus-privacy.eu.org/api/search/"
    const val EXODUS_TRACKERS_URL = "https://reports.exodus-privacy.eu.org/api/trackers"

    const val PLEXUS_API_URL = "https://plexus.techlore.tech/api/v1/apps"
    const val PLEXUS_SEARCH_URL = "https://plexus.techlore.tech/?q="

    const val SHARE_URL = "https://play.google.com/store/apps/details?id="

    const val UPDATE_URL_VANILLA =
        "https://auroraoss.com/downloads/AuroraStore/Feeds/release_feed.json"
    const val UPDATE_URL_NIGHTLY =
        "https://auroraoss.com/downloads/AuroraStore/Feeds/nightly_feed.json"

    // Channel IDs carry a version suffix where the importance changed from a previous
    // release: Android ignores importance edits on an already-created channel, so a new ID
    // is the only way to roll out a lower importance. Retired IDs are listed in
    // [LEGACY_NOTIFICATION_CHANNELS] so they can be deleted on next launch.
    const val NOTIFICATION_CHANNEL_EXPORT = "NOTIFICATION_CHANNEL_EXPORT_V2"
    const val NOTIFICATION_CHANNEL_INSTALL = "NOTIFICATION_CHANNEL_INSTALLED"
    const val NOTIFICATION_CHANNEL_DOWNLOADS = "NOTIFICATION_CHANNEL_DOWNLOADS"
    const val NOTIFICATION_CHANNEL_UPDATES = "NOTIFICATION_CHANNEL_UPDATES"
    const val NOTIFICATION_CHANNEL_ALERTS = "NOTIFICATION_CHANNEL_ALERTS"

    // Channels removed or superseded by a higher-versioned ID; deleted on next launch.
    val LEGACY_NOTIFICATION_CHANNELS = listOf(
        "NOTIFICATION_CHANNEL_EXPORT",
        "NOTIFICATION_CHANNEL_INSTALL",
        "NOTIFICATION_CHANNEL_ACCOUNT"
    )

    const val GITLAB_URL = "https://gitlab.com/AuroraOSS/AuroraStore"
    const val URL_DISPENSER = "https://auroraoss.com/api/auth"

    // ACCOUNTS
    const val ACCOUNT_SIGNED_IN = "ACCOUNT_SIGNED_IN"
    const val ACCOUNT_TYPE = "ACCOUNT_TYPE"
    const val ACCOUNT_EMAIL_PLAIN = "ACCOUNT_EMAIL_PLAIN"
    const val ACCOUNT_AAS_PLAIN = "ACCOUNT_AAS_PLAIN"
    const val ACCOUNT_AUTH_PLAIN = "ACCOUNT_AUTH_PLAIN"

    const val PAGE_TYPE = "PAGE_TYPE"
    const val TOP_CHART_TYPE = "TOP_CHART_TYPE"
    const val TOP_CHART_CATEGORY = "TOP_CHART_CATEGORY"

    const val JSON_MIME_TYPE = "application/json"
    const val PROPERTIES_IMPORT_MIME_TYPE = "application/octet-stream"
    const val PROPERTIES_EXPORT_MIME_TYPE = "text/x-java-properties"

    // PACKAGE NAMES
    const val PACKAGE_NAME_GMS = "com.google.android.gms"
    const val PACKAGE_NAME_PLAY_STORE = "com.android.vending"
    const val PACKAGE_NAME_APP_GALLERY = "com.huawei.appmarket"

    // FLAVOURS
    const val FLAVOUR_VANILLA = "vanilla"
    const val FLAVOUR_HUAWEI = "huawei"
    const val FLAVOUR_PRELOAD = "preload"
}
