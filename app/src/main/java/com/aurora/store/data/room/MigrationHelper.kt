package com.aurora.store.data.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A helper class for doing migrations for the [AuroraDatabase].
 * @see [RoomModule]
 */
object MigrationHelper {

    // ADD ALL NEW MIGRATION STEPS HERE TOO
    val MIGRATION_1_4 = object : Migration(1, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateFrom3To4(db)
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateFrom3To4(db)
    }

    /**
     * Add targetSdk column to download and update table for checking if silent install is possible.
     */
    private fun migrateFrom3To4(db: SupportSQLiteDatabase) {
        db.apply {
            execSQL("ALTER TABLE download ADD COLUMN targetSdk INTEGER")
            execSQL("ALTER TABLE update ADD COLUMN targetSdk INTEGER")
        }
    }
}
