package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    suspend fun getAllRecordingsOnce(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun getRecordingById(id: Long): Flow<RecordingEntity?>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingByIdOnce(id: Long): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)

    @Query("DELETE FROM recordings WHERE id IN (:ids)")
    suspend fun deleteRecordingsByIds(ids: List<Long>)

    @Query("SELECT * FROM recordings WHERE filePath = :filePath LIMIT 1")
    suspend fun getRecordingByFilePath(filePath: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getRecordingBySessionId(sessionId: String): RecordingEntity?

    @Query("UPDATE recordings SET title = :newTitle WHERE id = :id")
    suspend fun updateTitle(id: Long, newTitle: String)

    @Query("UPDATE recordings SET isCloudBackedUp = :isBackedUp, isEncrypted = :isEncrypted, cloudBackupDate = :backupDate, cloudStorageId = :storageId WHERE id = :id")
    suspend fun updateCloudStatus(
        id: Long,
        isBackedUp: Boolean,
        isEncrypted: Boolean,
        backupDate: Long,
        storageId: String
    )

    @Query("SELECT * FROM recordings WHERE isFirebaseSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedRecordings(): List<RecordingEntity>

    @Query("UPDATE recordings SET isFirebaseSynced = 1, firebaseSyncTimestamp = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE recordings SET isFirebaseSynced = 1, firebaseSyncTimestamp = :timestamp WHERE id IN (:ids)")
    suspend fun markMultipleAsSynced(ids: List<Long>, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM recordings WHERE isFirebaseSynced = 0")
    fun getUnsyncedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recordings WHERE isFirebaseSynced = 1")
    fun getSyncedCountFlow(): Flow<Int>
}
