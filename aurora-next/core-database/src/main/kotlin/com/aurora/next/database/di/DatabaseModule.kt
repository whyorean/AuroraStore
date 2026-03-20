package com.aurora.next.database.di

import android.content.Context
import androidx.room.Room
import com.aurora.next.database.AppDatabase
import com.aurora.next.database.dao.AppDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "aurora_next.db"
        ).build()
    }

    @Provides
    fun provideAppDao(database: AppDatabase): AppDao = database.appDao()
}
