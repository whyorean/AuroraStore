package com.aurora.store.data.room.update

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aurora.store.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(updates: List<Update>)

    @Query("SELECT * FROM `update` ORDER BY displayName ASC")
    fun updates(): Flow<List<Update>>

    @Query("DELETE FROM `update` WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM `update`")
    suspend fun deleteAll()

    @Transaction
    suspend fun insertUpdates(updates: List<Update>) {
        deleteAll()
        insertAll(updates)
    }

    @Transaction
    @Query(
        """
            SELECT * FROM `update`
            LEFT JOIN download ON `update`.packageName = `download`.packageName
            ORDER BY `update`.displayName ASC
        """
    )
    fun pagedUpdates(): PagingSource<Int, UpdateWithDownload>

    @Query(
        """
            SELECT EXISTS(
                SELECT 1 FROM `update`
                INNER JOIN download ON `update`.packageName = `download`.packageName
                WHERE `download`.downloadStatus IN (:status)
            )
        """
    )
    fun hasOngoingUpdates(status: List<DownloadStatus> = DownloadStatus.running): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM `update`")
    fun updatesCount(): Flow<Int>
}
