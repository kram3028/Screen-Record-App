package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.RecordingEntity
import java.io.File

object ShareUtils {

    /**
     * Shares a recorded video file via Android's native share sheet.
     * Uses FileProvider for real media files and falls back to metadata sharing if needed.
     */
    fun shareRecording(context: Context, recording: RecordingEntity) {
        try {
            val file = File(recording.filePath)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val hasValidFile = file.exists() && file.length() > 0

            if (hasValidFile) {
                try {
                    val authority = "${context.packageName}.fileprovider"
                    val contentUri = FileProvider.getUriForFile(context, authority, file)

                    val mimeType = when {
                        recording.format.equals("MKV", ignoreCase = true) || file.name.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
                        else -> "video/mp4"
                    }

                    shareIntent.type = mimeType
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, recording.title)
                    shareIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        "Check out this screen recording: ${recording.title} (${recording.resolution}, ${recording.fps} FPS)"
                    )
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    // Fallback to text summary
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, recording.title)
                    shareIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        "Screen Recording: ${recording.title}\nResolution: ${recording.resolution}\nFormat: ${recording.format}\nDuration: ${formatDuration(recording.durationMs)}"
                    )
                }
            } else {
                // Fallback text summary
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, recording.title)
                shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Screen Recording: ${recording.title}\nResolution: ${recording.resolution}\nFormat: ${recording.format}\nDuration: ${formatDuration(recording.durationMs)}"
                )
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Screen Recording via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not open share menu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDuration(millis: Long): String {
        val sec = millis / 1000
        val min = sec / 60
        val remSec = sec % 60
        return String.format("%02d:%02d", min, remSec)
    }
}
