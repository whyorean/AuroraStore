/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables.preview

import android.Manifest
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.gplayapi.data.models.Rating
import com.aurora.gplayapi.data.models.details.TestingProgram
import com.aurora.store.BuildConfig

/**
 * Preview provider for composable working with [App]
 */
class AppPreviewProvider : PreviewParameterProvider<App> {
    companion object {
        private const val CHANGELOG = """
            • New app compatibility ratings powered by Plexus<br>
            • Improvements to blacklist manager<br>
            • Ability to change auto-update restrictions<br>
            • Minor bug fixes and improvements<br>
            • Translation updates; additional strings localized
        """

        private const val DESCRIPTION = """
            <p>Aurora Store is an unofficial, FOSS client to Google Play with an elegant design. Aurora Store allows users to download, update, and search for apps like the Play Store. It works perfectly fine with or without Google Play Services or microG.</p>
            
            <p><strong>Features:</strong></p>
            
            <p>• FOSS: Has GPLv3 licence<br>
            • Beautiful design: Built upon latest Material 3 guidelines<br>
            • Account login: You can login with either personal or an anonymous account<br>
            • Device &amp; Locale spoofing: Change your device and/or locale to access geo locked apps<br>
            • Exodus Privacy integration: Instantly see trackers in app<br>
            • Plexus integration: Instantly see app compatibility without Google Play Services or with microG<br>
            • Updates blacklisting: Ignore updates for specific apps</p>
        """
    }

    override val values: Sequence<App>
        get() = sequenceOf(
            App(
                packageName = BuildConfig.APPLICATION_ID,
                displayName = "Aurora Store",
                developerName = "Rahul Kumar Patel",
                versionCode = BuildConfig.VERSION_CODE,
                versionName = BuildConfig.VERSION_NAME,
                shortDescription = "An unofficial FOSS client to Google Play with an elegant design and privacy",
                changes = CHANGELOG,
                description = DESCRIPTION,
                isFree = true,
                containsAds = false,
                isInstalled = true,
                size = 7431013,
                updatedOn = "Mar 17, 2025",
                labeledRating = "4.3",
                installs = 1000000000,
                developerEmail = "rahul@auroraoss.com",
                developerWebsite = "https://auroraoss.com/",
                developerAddress = "330 N Midland Ave, Mumbai, India",
                screenshots = MutableList(5) { Artwork(url = "$it") },
                testingProgram = TestingProgram(
                    isAvailable = true,
                    isSubscribed = false
                ),
                rating = Rating(
                    fiveStar = 201458104,
                    fourStar = 313829104,
                    threeStar = 204581672,
                    twoStar = 183746829,
                    oneStar = 96384291,
                    average = 4.4F,
                    abbreviatedLabel = "10 M"
                ),
                permissions = mutableListOf(
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        )
}
