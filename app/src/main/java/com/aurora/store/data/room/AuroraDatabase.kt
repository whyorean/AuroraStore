package com.aurora.store.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.data.room.download.DownloadConverter

@Database(entities = [Download::class], version = 1, exportSchema = false)
@TypeConverters(DownloadConverter::class)
abstract class AuroraDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
