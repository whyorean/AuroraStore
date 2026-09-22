/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aurora.store.data.room.account.Account
import com.aurora.store.data.room.account.AccountConverter
import com.aurora.store.data.room.account.AccountDao
import com.aurora.store.data.room.account.AppAccountBinding
import com.aurora.store.data.room.account.AppAccountBindingDao
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.DownloadConverter
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.data.room.exodus.TrackerDao
import com.aurora.store.data.room.exodus.TrackerEntity
import com.aurora.store.data.room.favourite.Favourite
import com.aurora.store.data.room.favourite.FavouriteDao
import com.aurora.store.data.room.notification.AppNotification
import com.aurora.store.data.room.notification.NotificationDao
import com.aurora.store.data.room.review.LocalReview
import com.aurora.store.data.room.review.ReviewDao
import com.aurora.store.data.room.update.IgnoredUpdate
import com.aurora.store.data.room.update.IgnoredUpdateDao
import com.aurora.store.data.room.update.Update
import com.aurora.store.data.room.update.UpdateDao

@Database(
    entities = [
        Download::class,
        Favourite::class,
        Update::class,
        IgnoredUpdate::class,
        LocalReview::class,
        Account::class,
        AppAccountBinding::class,
        TrackerEntity::class,
        AppNotification::class
    ],
    version = 13,
    exportSchema = true
)
@TypeConverters(DownloadConverter::class, AccountConverter::class)
abstract class AuroraDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun updateDao(): UpdateDao
    abstract fun ignoredUpdateDao(): IgnoredUpdateDao
    abstract fun reviewDao(): ReviewDao
    abstract fun accountDao(): AccountDao
    abstract fun appAccountBindingDao(): AppAccountBindingDao
    abstract fun trackerDao(): TrackerDao
    abstract fun notificationDao(): NotificationDao
}
