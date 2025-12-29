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

    private const val TAG = "MigrationHelper"

    private fun migrateFrom1To2(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL("CREATE TABLE `favourite` (`packageName` TEXT NOT NULL, `displayName` TEXT NOT NULL, `iconURL` TEXT NOT NULL, `added` INTEGER NOT NULL, `mode` TEXT NOT NULL, PRIMARY KEY(`packageName`))")
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
            database.execSQL("CREATE TABLE `update` (`packageName` TEXT NOT NULL, `versionCode` INTEGER NOT NULL, `versionName` TEXT NOT NULL, `displayName` TEXT NOT NULL, `iconURL` TEXT NOT NULL, `changelog` TEXT NOT NULL, `id` INTEGER NOT NULL, `developerName` TEXT NOT NULL, `size` INTEGER NOT NULL, `updatedOn` TEXT NOT NULL, `hasValidCert` INTEGER NOT NULL, `offerType` INTEGER NOT NULL, `fileList` TEXT NOT NULL, `sharedLibs` TEXT NOT NULL, PRIMARY KEY(`packageName`))")
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
                database.execSQL("ALTER TABLE `$it` ADD COLUMN targetSdk INTEGER NOT NULL DEFAULT 1")
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
            database.execSQL("ALTER TABLE `download` ADD COLUMN downloadedAt INTEGER NOT NULL DEFAULT 0")
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
}
