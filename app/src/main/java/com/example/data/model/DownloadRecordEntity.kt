package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores secure audit records for all downloaded/exported videos in local SQLite (Room),
 * including SHA-256 integrity checksums for tampering verification.
 */
@Entity(tableName = "download_records")
data class DownloadRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordingId: Long,
    val title: String,
    val destinationPath: String,
    val contentUri: String,
    val fileSizeBytes: Long,
    val downloadedAt: Long = System.currentTimeMillis(),
    val rewardAdVerified: Boolean = true,
    val integrityHashSha256: String = ""
)
