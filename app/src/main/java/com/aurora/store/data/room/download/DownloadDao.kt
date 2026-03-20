package com.aurora.store.data.room.download

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.store.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download)

    @Query("UPDATE download SET downloadStatus=:downloadStatus WHERE packageName=:packageName")
    suspend fun updateStatus(packageName: String, downloadStatus: DownloadStatus)

    @Query("UPDATE download SET fileList=:fileList WHERE packageName=:packageName")
    suspend fun updateFiles(packageName: String, fileList: List<PlayFile>)

    @Query("UPDATE download SET sharedLibs=:sharedLibs WHERE packageName=:packageName")
    suspend fun updateSharedLibs(packageName: String, sharedLibs: List<SharedLib>)

    @Query(
        """
        UPDATE download
        SET progress=:progress, speed=:speed, timeRemaining=:timeRemaining
        WHERE packageName=:packageName
        """
    )
    suspend fun updateProgress(packageName: String, progress: Int, speed: Long, timeRemaining: Long)

    @Query("SELECT * FROM download")
    fun downloads(): Flow<List<Download>>

    @Query("SELECT * FROM download ORDER BY downloadedAt DESC")
    fun pagedDownloads(): PagingSource<Int, Download>

    @Query("SELECT * FROM download WHERE packageName = :packageName")
    suspend fun getDownload(packageName: String): Download

    @Query("DELETE FROM download WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM download")
    suspend fun deleteAll()
}
