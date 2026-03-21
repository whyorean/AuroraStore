package com.jmods.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jmods.database.dao.AppDao
import com.jmods.database.entity.AppEntity

@Database(entities = [AppEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
