package com.aurora.next.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aurora.next.database.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps WHERE category = :category")
    fun getAppsByCategory(category: String): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    fun getAppByPackageName(packageName: String): Flow<AppEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)

    @Query("DELETE FROM apps WHERE category = :category")
    suspend fun deleteByCategory(category: String)
}
