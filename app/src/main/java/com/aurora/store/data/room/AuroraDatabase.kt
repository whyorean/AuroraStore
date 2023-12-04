package com.aurora.store.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.DownloadDao

@Database(entities = [Download::class], version = 1, exportSchema = false)
abstract class AuroraDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
