package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TorrentDao {
    @Query("SELECT * FROM torrents ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<TorrentEntity>>

    @Query("SELECT * FROM torrents WHERE infoHash = :infoHash LIMIT 1")
    fun observeByHash(infoHash: String): Flow<TorrentEntity?>

    @Query("SELECT * FROM torrents")
    suspend fun getAllOnce(): List<TorrentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TorrentEntity)

    @Query(
        "UPDATE torrents SET name = :name, totalBytes = :totalBytes, downloadedBytes = :downloadedBytes, " +
            "uploadedBytes = :uploadedBytes, progress = :progress, statusName = :statusName, " +
            "completedAt = :completedAt, errorMessage = :errorMessage WHERE infoHash = :infoHash"
    )
    suspend fun updateProgress(
        infoHash: String,
        name: String,
        totalBytes: Long,
        downloadedBytes: Long,
        uploadedBytes: Long,
        progress: Float,
        statusName: String,
        completedAt: Long?,
        errorMessage: String?
    )

    @Query("UPDATE torrents SET statusName = :statusName WHERE infoHash = :infoHash")
    suspend fun updateStatus(infoHash: String, statusName: String)

    @Query("DELETE FROM torrents WHERE infoHash = :infoHash")
    suspend fun deleteByHash(infoHash: String)
}
