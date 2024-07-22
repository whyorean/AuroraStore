package com.aurora.store.data.room.favourites

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favourite: Favourite)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favourites: List<Favourite>)

    @Query("SELECT * FROM favourite")
    fun favourites(): Flow<List<Favourite>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourite WHERE packageName = :packageName)")
    suspend fun isFavourite(packageName: String): Boolean

    @Query("DELETE FROM favourite WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM favourite")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM favourite")
    suspend fun count(): Int
}
