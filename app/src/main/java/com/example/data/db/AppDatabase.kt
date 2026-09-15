package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.RecordingDao
import com.example.data.dao.UserDataDao
import com.example.data.model.DownloadRecordEntity
import com.example.data.model.RecordingEntity
import com.example.data.model.UserDataEntity

@Database(
    entities = [RecordingEntity::class, UserDataEntity::class, DownloadRecordEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun userDataDao(): UserDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Clear any legacy Firebase/Firestore sync preferences from persistent storage
                try {
                    context.applicationContext.getSharedPreferences("firebase_sync_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply()
                } catch (_: Exception) {}

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "screen_recorder.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
