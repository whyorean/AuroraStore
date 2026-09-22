/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-FileCopyrightText: 2023 grrfe <grrfe@420blaze.it>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log.DEBUG
import android.util.Log.INFO
import androidx.compose.material3.ComposeMaterial3Flags
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.aurora.extensions.setAppTheme
import com.aurora.store.data.event.EventFlow
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.helper.UpdateHelper
import com.aurora.store.data.receiver.PackageManagerReceiver
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.MainScope
import okhttp3.OkHttpClient

@HiltAndroidApp
class AuroraApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var downloadHelper: DownloadHelper

    @Inject
    lateinit var updateHelper: UpdateHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) DEBUG else INFO)
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        var scope = MainScope()
            private set

        val enqueuedInstalls: MutableSet<String> = mutableSetOf()
        val events = EventFlow()

        private var startedActivities = 0

        val isForeground get() = startedActivities > 0
    }

    override fun onCreate() {
        ComposeMaterial3Flags.isCheckboxStylingFixEnabled = true
        super.onCreate()
        // Set the app theme
        val themeStyle = Preferences.getInteger(this, Preferences.PREFERENCE_THEME_STYLE)
        setAppTheme(themeStyle)

        // Apply dynamic colors to activities, unless disabled (opt-out, off by default on One UI)
        val dynamicColors = Preferences.getBoolean(
            this,
            Preferences.PREFERENCE_DYNAMIC_COLORS,
            Preferences.dynamicColorsDefault
        )
        if (dynamicColors) DynamicColors.applyToActivitiesIfAvailable(this)

        // Create Notification Channels
        NotificationUtil.createNotificationChannel(this)

        registerActivityLifecycleCallbacks(foregroundTracker)

        // Initialize Download and Update helpers to observe and trigger downloads
        downloadHelper.init()
        updateHelper.init()

        // Register broadcast receiver for package install/uninstall
        ContextCompat.registerReceiver(
            this,
            object : PackageManagerReceiver() {},
            PackageUtil.getFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        CommonUtil.cleanupInstallationSessions(applicationContext)
    }

    private val foregroundTracker = object : ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            startedActivities++
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivities = (startedActivities - 1).coerceAtLeast(0)
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader(this).newBuilder()
        .crossfade(true)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = okHttpClient)) }
        .build()
}
