package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recordings",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["sessionId"])
    ]
)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val resolution: String,      // e.g. "1080p (FHD)", "720p (HD)", "2K"
    val format: String,          // "MP4" or "MKV"
    val fps: Int,                // 30, 60, 120
    val bitrateMbps: Int,        // 4, 8, 16, 24
    val audioSource: String,     // "Mic + System Audio", "Microphone", "System Audio", "Muted"
    val createdAt: Long = System.currentTimeMillis(),
    val isCloudBackedUp: Boolean = false,
    val isEncrypted: Boolean = false,
    val cloudBackupDate: Long = 0L,
    val cloudStorageId: String = "",
    val sessionId: String = "",
    val isFirebaseSynced: Boolean = false,
    val firebaseSyncTimestamp: Long = 0L
)
