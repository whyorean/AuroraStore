package com.aurora.next.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aurora.next.database.dao.AppDao
import com.aurora.next.database.entity.AppEntity

@Database(entities = [AppEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
