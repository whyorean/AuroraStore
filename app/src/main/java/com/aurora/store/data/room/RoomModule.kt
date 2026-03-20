package com.aurora.store.data.room

import android.content.Context
import androidx.room.Room
import com.aurora.store.data.room.MigrationHelper.MIGRATION_1_2
import com.aurora.store.data.room.MigrationHelper.MIGRATION_2_3
import com.aurora.store.data.room.MigrationHelper.MIGRATION_3_4
import com.aurora.store.data.room.MigrationHelper.MIGRATION_4_5
import com.aurora.store.data.room.MigrationHelper.MIGRATION_5_6
import com.aurora.store.data.room.download.DownloadConverter
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.data.room.favourite.FavouriteDao
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
    ): AuroraDatabase = Room.databaseBuilder(context, AuroraDatabase::class.java, DATABASE)
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6
        )
        .addTypeConverter(downloadConverter)
        .build()

    @Provides
    fun providesDownloadDao(auroraDatabase: AuroraDatabase): DownloadDao =
        auroraDatabase.downloadDao()

    @Provides
    fun providesFavouriteDao(auroraDatabase: AuroraDatabase): FavouriteDao =
        auroraDatabase.favouriteDao()

    @Provides
    fun providesUpdateDao(auroraDatabase: AuroraDatabase): UpdateDao = auroraDatabase.updateDao()
}
