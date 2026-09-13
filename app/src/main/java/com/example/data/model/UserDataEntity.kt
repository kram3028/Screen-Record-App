package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores user security profile and application usage metrics in the local SQLite database.
 */
@Entity(tableName = "user_security_profile")
data class UserDataEntity(
    @PrimaryKey
    val userId: String = "local_default_user",
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val totalSavedVideosCount: Int = 0,
    val rewardedAdsWatched: Int = 0,
    val totalExportedBytes: Long = 0L,
    val isCloudVaultEncrypted: Boolean = true,
    val securityTokenHash: String = ""
)
