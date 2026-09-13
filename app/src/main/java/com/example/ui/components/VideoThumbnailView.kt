package com.example.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecordingEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RecorderRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Memory cache for video frame thumbnails to keep gallery grid scrolling butter smooth
private val thumbnailCache = object : LruCache<String, Bitmap>(32) {}

@Composable
fun VideoThumbnailView(
    recording: RecordingEntity,
    modifier: Modifier = Modifier,
    showPlayOverlay: Boolean = true,
    showBadges: Boolean = true
) {
    var thumbnailBitmap by remember(recording.filePath, recording.id) {
        mutableStateOf(thumbnailCache.get(recording.filePath))
    }

    LaunchedEffect(recording.filePath, recording.id) {
        if (thumbnailBitmap == null) {
            val loadedBitmap = withContext(Dispatchers.IO) {
                loadVideoFrame(recording.filePath, recording.durationMs)
            }
            if (loadedBitmap != null) {
                thumbnailCache.put(recording.filePath, loadedBitmap)
                thumbnailBitmap = loadedBitmap
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF14171F)),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailBitmap != null) {
            // Real extracted video frame
            Image(
                bitmap = thumbnailBitmap!!.asImageBitmap(),
                contentDescription = "Video Thumbnail for ${recording.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_thumbnail_image_${recording.id}")
            )

            // Scrim overlay to ensure badges and play button are clearly legible
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
        } else {
            // Styled aesthetic fallback thumbnail with dynamic colorway & cinematic HUD elements
            val gradientColors = remember(recording.id, recording.format) {
                when {
                    recording.format.equals("MKV", ignoreCase = true) -> listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                    recording.isCloudBackedUp -> listOf(
                        Color(0xFF132F27),
                        Color(0xFF1A4731),
                        Color(0xFF162521)
                    )
                    recording.id % 3L == 0L -> listOf(
                        Color(0xFF2E112D),
                        Color(0xFF540032),
                        Color(0xFF190019)
                    )
                    recording.id % 2L == 0L -> listOf(
                        Color(0xFF1B1B2F),
                        Color(0xFF162447),
                        Color(0xFF1F4068)
                    )
                    else -> listOf(
                        Color(0xFF231F20),
                        Color(0xFF380036),
                        Color(0xFF1A1A24)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(gradientColors))
            )

            // High-tech viewfinder corner markers
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                val strokeW = 1.5.dp.toPx()
                val cornerL = 12.dp.toPx()
                val alpha = 0.35f
                val c = Color.White.copy(alpha = alpha)

                // Top-Left
                drawLine(c, Offset(0f, 0f), Offset(cornerL, 0f), strokeW)
                drawLine(c, Offset(0f, 0f), Offset(0f, cornerL), strokeW)

                // Top-Right
                drawLine(c, Offset(size.width - cornerL, 0f), Offset(size.width, 0f), strokeW)
                drawLine(c, Offset(size.width, 0f), Offset(size.width, cornerL), strokeW)

                // Bottom-Left
                drawLine(c, Offset(0f, size.height), Offset(cornerL, size.height), strokeW)
                drawLine(c, Offset(0f, size.height - cornerL), Offset(0f, size.height), strokeW)

                // Bottom-Right
                drawLine(c, Offset(size.width - cornerL, size.height), Offset(size.width, size.height), strokeW)
                drawLine(c, Offset(size.width, size.height - cornerL), Offset(size.width, size.height), strokeW)
            }
        }

        // Center Play Button Overlay
        if (showPlayOverlay) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("thumbnail_play_overlay_${recording.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        if (showBadges) {
            // Top-Left: Format Badge (MP4 vs MKV)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (recording.format.equals("MKV", ignoreCase = true)) CyberCyan else RecorderRed
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = recording.format,
                    color = if (recording.format.equals("MKV", ignoreCase = true)) Color.Black else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Top-Right: Cloud/Vault or Encryption Icon
            if (recording.isCloudBackedUp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (recording.isEncrypted) Icons.Default.Lock else Icons.Default.CloudDone,
                            contentDescription = "Cloud Status",
                            tint = NeonGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (recording.isEncrypted) "VAULT" else "CLOUD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                    }
                }
            }

            // Bottom-Left: Resolution badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = recording.resolution.substringBefore(" "),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Bottom-Right: Duration Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(recording.durationMs),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun loadVideoFrame(filePath: String, durationMs: Long): Bitmap? {
    var retriever: MediaMetadataRetriever? = null
    return try {
        val file = File(filePath)
        if (!file.exists() || file.length() < 1024) {
            return null
        }
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)

        // Request frame at 1s or 25% duration
        val frameTimeUs = if (durationMs > 2000L) 1_000_000L else 200_000L
        retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.frameAtTime
    } catch (e: Exception) {
        null
    } finally {
        try {
            retriever?.release()
        } catch (e: Exception) {
            // Ignore release exceptions
        }
    }
}

private fun formatDuration(millis: Long): String {
    val sec = millis / 1000
    val min = sec / 60
    val remSec = sec % 60
    return String.format("%02d:%02d", min, remSec)
}
