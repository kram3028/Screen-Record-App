package com.example.recorder

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thread-safe file writer managing the MediaMuxer instance and file lifecycle
 * for a unique recording session. Guarantees that the MediaMuxer instance is
 * properly stopped and closed to prevent corrupted streams and redundant file copies.
 */
class RecordFileWriter(
    val sessionId: String,
    val outputFile: File,
    outputFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
) {
    companion object {
        private const val TAG = "RecordFileWriter"
    }

    private val isClosed = AtomicBoolean(false)
    private val isStarted = AtomicBoolean(false)
    private var mediaMuxer: MediaMuxer? = null

    init {
        try {
            outputFile.parentFile?.mkdirs()
            mediaMuxer = MediaMuxer(outputFile.absolutePath, outputFormat)
            Log.d(TAG, "RecordFileWriter initialized for session $sessionId -> ${outputFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaMuxer for session $sessionId", e)
            mediaMuxer = null
        }
    }

    fun isStarted(): Boolean = isStarted.get()
    fun isClosed(): Boolean = isClosed.get()

    @Synchronized
    fun addTrack(format: MediaFormat): Int {
        if (isClosed.get()) {
            Log.w(TAG, "Cannot addTrack: writer for session $sessionId is closed")
            return -1
        }
        val muxer = mediaMuxer ?: return -1
        return try {
            muxer.addTrack(format)
        } catch (e: Exception) {
            Log.e(TAG, "addTrack failed for session $sessionId", e)
            -1
        }
    }

    @Synchronized
    fun start() {
        if (isClosed.get()) {
            Log.w(TAG, "Cannot start: writer for session $sessionId is closed")
            return
        }
        val muxer = mediaMuxer ?: return
        if (isStarted.compareAndSet(false, true)) {
            try {
                muxer.start()
                Log.d(TAG, "MediaMuxer successfully started for session $sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "MediaMuxer start failed for session $sessionId", e)
            }
        }
    }

    @Synchronized
    fun writeSampleData(trackIndex: Int, byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (isClosed.get() || !isStarted.get()) return
        val muxer = mediaMuxer ?: return
        try {
            muxer.writeSampleData(trackIndex, byteBuffer, bufferInfo)
        } catch (e: Exception) {
            Log.e(TAG, "writeSampleData failed for session $sessionId", e)
        }
    }

    /**
     * Properly stops and releases the MediaMuxer instance.
     * Guaranteed to be idempotent and safe across multiple calls.
     */
    @Synchronized
    fun closeMuxer(): Boolean {
        if (isClosed.compareAndSet(false, true)) {
            Log.d(TAG, "Closing MediaMuxer for session $sessionId...")
            val muxer = mediaMuxer
            try {
                if (muxer != null && isStarted.get()) {
                    try {
                        muxer.stop()
                        Log.d(TAG, "MediaMuxer stopped successfully for session $sessionId")
                    } catch (e: Exception) {
                        Log.w(TAG, "MediaMuxer.stop() exception ignored for session $sessionId: ${e.message}")
                    }
                }
                if (muxer != null) {
                    try {
                        muxer.release()
                        Log.d(TAG, "MediaMuxer released successfully for session $sessionId")
                    } catch (e: Exception) {
                        Log.w(TAG, "MediaMuxer.release() exception ignored for session $sessionId: ${e.message}")
                    }
                }
            } finally {
                mediaMuxer = null
                Log.d(TAG, "MediaMuxer cleanup completed for session $sessionId")
            }
            return true
        }
        return false
    }
}
