package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DownloadRecordEntity
import com.example.data.model.UserDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDataDao {

    @Query("SELECT * FROM user_security_profile WHERE userId = :userId LIMIT 1")
    fun getUserDataFlow(userId: String = "local_default_user"): Flow<UserDataEntity?>

    @Query("SELECT * FROM user_security_profile WHERE userId = :userId LIMIT 1")
    suspend fun getUserData(userId: String = "local_default_user"): UserDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(userData: UserDataEntity)

    @Query("UPDATE user_security_profile SET totalSavedVideosCount = totalSavedVideosCount + 1, rewardedAdsWatched = rewardedAdsWatched + 1, totalExportedBytes = totalExportedBytes + :bytes, lastActiveAt = :timestamp WHERE userId = :userId")
    suspend fun incrementDownloadMetrics(userId: String, bytes: Long, timestamp: Long = System.currentTimeMillis())

    // Download logs in SQL
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadRecord(record: DownloadRecordEntity): Long

    @Query("SELECT * FROM download_records ORDER BY downloadedAt DESC")
    fun getAllDownloadRecords(): Flow<List<DownloadRecordEntity>>

    @Query("SELECT COUNT(*) FROM download_records")
    suspend fun getDownloadCount(): Int
}
