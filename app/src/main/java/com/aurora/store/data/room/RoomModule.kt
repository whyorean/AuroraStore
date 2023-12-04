package com.aurora.store.data.room

import android.content.Context
import androidx.room.Room
import com.aurora.store.data.room.download.DownloadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    private const val DATABASE = "aurora_database"

    @Singleton
    @Provides
    fun providesRoomInstance(@ApplicationContext context: Context): AuroraDatabase {
        return Room.databaseBuilder(context, AuroraDatabase::class.java, DATABASE)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providesDownloadDao(auroraDatabase: AuroraDatabase): DownloadDao {
        return auroraDatabase.downloadDao()
    }
}
