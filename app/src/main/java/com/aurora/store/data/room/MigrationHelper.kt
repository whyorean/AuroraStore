/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A helper class for doing migrations for the [AuroraDatabase].
 * @see [RoomModule]
 */
object MigrationHelper {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom1To2(db)
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom2To3(db)
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom3To4(db)
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom4To5(db)
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom5To6(db)
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom6To7(db)
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom7To8(db)
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom8To9(db)
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom9To10(db)
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom10To11(db)
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom11To12(db)
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom12To13(db)
    }

    private const val TAG = "MigrationHelper"

    private fun migrateFrom1To2(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL(
                "CREATE TABLE `favourite` (`packageName` TEXT NOT NULL, `displayName` TEXT NOT NULL, `iconURL` TEXT NOT NULL, `added` INTEGER NOT NULL, `mode` TEXT NOT NULL, PRIMARY KEY(`packageName`))"
            )
            database.setTransactionSuccessful()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed while migrating from database version 1 to 2", exception)
        } finally {
            database.endTransaction()
        }
    }

    private fun migrateFrom2To3(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL(
                "CREATE TABLE `update` (`packageName` TEXT NOT NULL, `versionCode` INTEGER NOT NULL, `versionName` TEXT NOT NULL, `displayName` TEXT NOT NULL, `iconURL` TEXT NOT NULL, `changelog` TEXT NOT NULL, `id` INTEGER NOT NULL, `developerName` TEXT NOT NULL, `size` INTEGER NOT NULL, `updatedOn` TEXT NOT NULL, `hasValidCert` INTEGER NOT NULL, `offerType` INTEGER NOT NULL, `fileList` TEXT NOT NULL, `sharedLibs` TEXT NOT NULL, PRIMARY KEY(`packageName`))"
            )
            database.setTransactionSuccessful()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed while migrating from database version 2 to 3", exception)
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Add targetSdk column to download and update table for checking if silent install is possible.
     */
    private fun migrateFrom3To4(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            listOf("download", "update").forEach {
                database.execSQL(
                    "ALTER TABLE `$it` ADD COLUMN targetSdk INTEGER NOT NULL DEFAULT 1"
                )
            }
            database.setTransactionSuccessful()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed while migrating from database version 3 to 4", exception)
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Add downloadedAt column to download table for showing installation/update date of apps.
     */
    private fun migrateFrom4To5(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL(
                "ALTER TABLE `download` ADD COLUMN downloadedAt INTEGER NOT NULL DEFAULT 0"
            )
            database.setTransactionSuccessful()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed while migrating from database version 4 to 5", exception)
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Add requiresGMS column to download table for checking if app requires GMS to install.
     */
    private fun migrateFrom5To6(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL(
                "ALTER TABLE `download` ADD COLUMN requiresGMS INTEGER NOT NULL DEFAULT 0"
            )
            database.setTransactionSuccessful()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed while migrating from database version 5 to 6", exception)
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Add isIncompatible column to update table for flagging updates that cannot be applied
     * on the current OS (e.g. system app updates on HyperOS / GrapheneOS).
     */
    private fun migrateFrom6To7(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL(
                "ALTER TABLE `update` ADD COLUMN isIncompatible INTEGER NOT NULL DEFAULT 0"
            )
            database.setTransactionSuccessful()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed while migrating from database version 6 to 7", exception)
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Add ignored_update table for tracking updates that have been muted by the user.
     * A null ignoredVersionCode means all future updates for this package are hidden.
     */
    private fun migrateFrom7To8(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `ignored_update` (" +
                    "`packageName` TEXT NOT NULL, " +
                    "`ignoredVersionCode` INTEGER, " +
                    "PRIMARY KEY(`packageName`))"
            )
            database.setTransactionSuccessful()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed while migrating from database version 7 to 8", exception)
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Add review table for caching the user's own submitted reviews locally so they can be shown
     * immediately while Google takes time to publish them. Scoped by account e-mail.
     */
    private fun migrateFrom8To9(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `review` (" +
                    "`packageName` TEXT NOT NULL, " +
                    "`accountEmail` TEXT NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`comment` TEXT NOT NULL, " +
                    "`rating` INTEGER NOT NULL, " +
                    "`commentId` TEXT NOT NULL, " +
                    "`userName` TEXT NOT NULL, " +
                    "`userPhotoUrl` TEXT NOT NULL, " +
                    "`appVersion` TEXT NOT NULL, " +
                    "`timeStamp` INTEGER NOT NULL, " +
                    "`synced` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`packageName`, `accountEmail`))"
            )
            database.setTransactionSuccessful()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed while migrating from database version 8 to 9", exception)
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Add account & app_account_binding tables for multi-account support. The existing single
     * account is imported from SharedPreferences in code on first launch (see AccountRepository),
     * because a Room Migration cannot read SharedPreferences.
     */
    private fun migrateFrom9To10(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `account` (" +
                    "`id` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`email` TEXT NOT NULL, " +
                    "`displayName` TEXT, " +
                    "`profilePicUrl` TEXT, " +
                    "`aasToken` TEXT, " +
                    "`authToken` TEXT, " +
                    "`tokenType` TEXT NOT NULL, " +
                    "`authViaMicroG` INTEGER NOT NULL, " +
                    "`authDataJson` TEXT, " +
                    "`isDefault` INTEGER NOT NULL, " +
                    "`addedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))"
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `app_account_binding` (" +
                    "`packageName` TEXT NOT NULL, " +
                    "`accountId` TEXT NOT NULL, " +
                    "PRIMARY KEY(`packageName`), " +
                    "FOREIGN KEY(`accountId`) REFERENCES `account`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_app_account_binding_accountId` " +
                    "ON `app_account_binding` (`accountId`)"
            )
            database.setTransactionSuccessful()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed while migrating from database version 9 to 10", exception)
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Add the exodus_tracker table for the API-synced tracker dictionary.
     */
    private fun migrateFrom10To11(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `exodus_tracker` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`url` TEXT NOT NULL, " +
                    "`signature` TEXT NOT NULL, " +
                    "`date` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))"
            )
            database.setTransactionSuccessful()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed while migrating from database version 10 to 11", exception)
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Add a categories column to the exodus_tracker table, recreating it empty for the next sync.
     */
    private fun migrateFrom11To12(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL("DROP TABLE IF EXISTS `exodus_tracker`")
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `exodus_tracker` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`url` TEXT NOT NULL, " +
                    "`signature` TEXT NOT NULL, " +
                    "`date` TEXT NOT NULL, " +
                    "`categories` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))"
            )
            database.setTransactionSuccessful()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed while migrating from database version 11 to 12", exception)
        } finally {
            database.endTransaction()
        }
    }

    /**
     * Add the notification table backing the in-app notification centre.
     */
    private fun migrateFrom12To13(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `notification` (" +
                    "`id` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`body` TEXT NOT NULL, " +
                    "`packageName` TEXT, " +
                    "`url` TEXT, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`isRead` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))"
            )
            database.setTransactionSuccessful()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed while migrating from database version 12 to 13", exception)
        } finally {
            database.endTransaction()
        }
    }
}
