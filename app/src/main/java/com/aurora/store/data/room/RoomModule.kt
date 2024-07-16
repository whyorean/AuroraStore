package com.aurora.store.data.room

import android.content.Context
import androidx.room.Room
import com.aurora.store.data.room.download.DownloadConverter
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.data.room.favourites.FavouriteDao
import com.aurora.store.data.room.update.UpdateDao
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
    fun providesRoomInstance(
        @ApplicationContext context: Context,
        downloadConverter: DownloadConverter
    ): AuroraDatabase {
        return Room.databaseBuilder(context, AuroraDatabase::class.java, DATABASE)
            .addTypeConverter(downloadConverter)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providesDownloadDao(auroraDatabase: AuroraDatabase): DownloadDao {
        return auroraDatabase.downloadDao()
    }

    @Provides
    fun providesFavouriteDao(auroraDatabase: AuroraDatabase): FavouriteDao {
        return auroraDatabase.favouriteDao()
    }

    @Provides
    fun providesUpdateDao(auroraDatabase: AuroraDatabase): UpdateDao {
        return auroraDatabase.updateDao()
    }
}
