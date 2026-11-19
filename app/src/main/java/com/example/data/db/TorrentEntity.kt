package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "torrents")
data class TorrentEntity(
    @PrimaryKey val infoHash: String,
    val name: String,
    val sourceUri: String,
    val isTorrentFile: Boolean,
    val torrentFilePath: String?,
    val saveDir: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val uploadedBytes: Long,
    val progress: Float,
    val statusName: String,
    val addedAt: Long,
    val completedAt: Long?,
    val errorMessage: String?
)
