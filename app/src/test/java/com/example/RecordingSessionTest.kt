package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.RecordingEntity
import com.example.data.model.VideoFormat
import com.example.data.repository.RecordingRepository
import com.example.recorder.RecordFileWriter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RecordingSessionTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: RecordingRepository
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RecordingRepository(context, database.recordingDao())
        tempDir = File(context.cacheDir, "test_recordings_${System.currentTimeMillis()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        database.close()
        tempDir.deleteRecursively()
    }

    @Test
    fun recordFileWriter_closesMuxerIdempotently_andTracksSessionId() {
        val sessionId = "session_test_${UUID.randomUUID()}"
        val testFile = File(tempDir, "test_output.mp4")

        val writer = RecordFileWriter(
            sessionId = sessionId,
            outputFile = testFile
        )

        assertEquals(sessionId, writer.sessionId)
        assertEquals(testFile.absolutePath, writer.outputFile.absolutePath)
        assertFalse("Writer should not be closed initially", writer.isClosed())

        // Closing the muxer instance upon stopping recording
        val closedFirst = writer.closeMuxer()
        assertTrue("First closeMuxer call should return true", closedFirst)
        assertTrue("Writer should report isClosed as true", writer.isClosed())

        // Redundant / duplicate close calls must be idempotent to prevent duplicate operations or corruptions
        val closedSecond = writer.closeMuxer()
        assertFalse("Second closeMuxer call should be a no-op and return false", closedSecond)
    }

    @Test
    fun recordingRepository_deduplicatesBySessionId_andFilePath() = runBlocking {
        val sessionId = "session_unique_9988"
        val testFile = File(tempDir, "sample_vid.mp4").apply {
            writeBytes(ByteArray(1024) { 0x01 })
        }

        // First save
        val firstId = repository.saveRecording(
            title = "First Recording",
            file = testFile,
            durationMs = 5000L,
            resolution = "1080p",
            format = VideoFormat.MP4,
            fps = 60,
            bitrateMbps = 8,
            audioSource = "Microphone",
            sessionId = sessionId
        )
        assertTrue("First insertion should return valid ID > 0", firstId > 0L)

        // Attempt second save with identical sessionId -> must return existing ID, NO duplicate created
        val duplicateSessionId = repository.saveRecording(
            title = "Duplicate By Session",
            file = File(tempDir, "another_name.mp4"),
            durationMs = 5000L,
            resolution = "1080p",
            format = VideoFormat.MP4,
            fps = 60,
            bitrateMbps = 8,
            audioSource = "Microphone",
            sessionId = sessionId
        )
        assertEquals("Duplicate sessionId must return the existing record ID", firstId, duplicateSessionId)

        // Verify database only contains 1 record
        val allRecordings = database.recordingDao().getAllRecordingsOnce()
        assertEquals("Database must contain exactly 1 recording entity", 1, allRecordings.size)
        assertEquals("SessionId must match", sessionId, allRecordings[0].sessionId)
    }
}
