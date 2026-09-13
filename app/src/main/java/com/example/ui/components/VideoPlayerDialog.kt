package com.example.ui.components

import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.RecordingEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.RecorderRed
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun VideoPlayerDialog(
    recording: RecordingEntity,
    onDismiss: () -> Unit,
    onOpenTrimmer: (RecordingEntity) -> Unit,
    onShare: ((RecordingEntity) -> Unit)? = null,
    onDownload: ((RecordingEntity) -> Unit)? = null,
    onRename: ((String) -> Unit)? = null,
    isDownloading: Boolean = false
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isVideoPrepared by remember { mutableStateOf(false) }
    var hasPlaybackError by remember { mutableStateOf(false) }
    var isUserSeeking by remember { mutableStateOf(false) }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(recording.durationMs.coerceAtLeast(1000L)) }

    var showRenameDialog by remember { mutableStateOf(false) }

    val videoFile = remember(recording.filePath) { File(recording.filePath) }

    // Periodic progress poller
    LaunchedEffect(videoViewRef, isPlaying, isUserSeeking) {
        while (true) {
            if (isPlaying && !isUserSeeking) {
                videoViewRef?.let { vView ->
                    if (vView.isPlaying) {
                        val pos = vView.currentPosition.toLong()
                        currentPositionMs = pos
                        val dur = vView.duration.toLong()
                        if (dur > 0) {
                            totalDurationMs = dur
                        }
                    }
                }
            }
            delay(150)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {}
        }
    }

    if (showRenameDialog) {
        RenameRecordingDialog(
            recording = recording,
            onDismiss = { showRenameDialog = false },
            onConfirmRename = { newTitle ->
                showRenameDialog = false
                onRename?.invoke(newTitle)
            }
        )
    }

    Dialog(
        onDismissRequest = {
            try { videoViewRef?.stopPlayback() } catch (_: Exception) {}
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with title and rename option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = recording.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (onRename != null) {
                                IconButton(
                                    onClick = { showRenameDialog = true },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("player_rename_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename video",
                                        tint = RecorderRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${recording.resolution} • ${recording.format} • ${recording.fps} FPS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onDownload != null) {
                            IconButton(
                                onClick = { onDownload(recording) },
                                enabled = !isDownloading,
                                modifier = Modifier.testTag("player_download_button")
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = CyberCyan
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = "Save to phone",
                                        tint = CyberCyan
                                    )
                                }
                            }
                        }
                        if (onShare != null) {
                            IconButton(
                                onClick = { onShare(recording) },
                                modifier = Modifier.testTag("player_share_button")
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share video",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                try { videoViewRef?.stopPlayback() } catch (_: Exception) {}
                                onDismiss()
                            },
                            modifier = Modifier.testTag("player_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close player")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Video Surface Area with native VideoView
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoFile.exists() && !hasPlaybackError) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("native_video_view"),
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )
                                    videoViewRef = this
                                    setVideoPath(videoFile.absolutePath)
                                    setOnPreparedListener { mp ->
                                        isVideoPrepared = true
                                        val dur = mp.duration.toLong()
                                        if (dur > 0) {
                                            totalDurationMs = dur
                                        }
                                        mp.isLooping = false
                                        start()
                                        isPlaying = true
                                    }
                                    setOnCompletionListener {
                                        isPlaying = false
                                        currentPositionMs = totalDurationMs
                                    }
                                    setOnErrorListener { _, what, extra ->
                                        hasPlaybackError = true
                                        true
                                    }
                                }
                            }
                        )
                    } else {
                        // Fallback message when video file is missing or unreadable
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = RecorderRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Recording file not found or corrupted",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = videoFile.name,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Click overlay to toggle play/pause
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                videoViewRef?.let { vView ->
                                    if (vView.isPlaying) {
                                        vView.pause()
                                        isPlaying = false
                                    } else {
                                        if (currentPositionMs >= totalDurationMs - 300) {
                                            vView.seekTo(0)
                                            currentPositionMs = 0L
                                        }
                                        vView.start()
                                        isPlaying = true
                                    }
                                }
                            }
                    )

                    // Top badges inside video preview
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (recording.isEncrypted) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF004D40))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("AES-256", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(RecorderRed.copy(alpha = 0.85f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = recording.format,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress scrub bar
                val sliderValue = currentPositionMs.toFloat().coerceIn(0f, totalDurationMs.toFloat())
                Slider(
                    value = sliderValue,
                    valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1000f),
                    onValueChange = {
                        isUserSeeking = true
                        currentPositionMs = it.toLong()
                    },
                    onValueChangeFinished = {
                        videoViewRef?.seekTo(currentPositionMs.toInt())
                        isUserSeeking = false
                        if (isPlaying) {
                            videoViewRef?.start()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_progress_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = RecorderRed,
                        activeTrackColor = RecorderRed
                    )
                )

                // Time indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPositionMs),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatTime(totalDurationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val target = (currentPositionMs - 5000L).coerceAtLeast(0L)
                            videoViewRef?.seekTo(target.toInt())
                            currentPositionMs = target
                        },
                        modifier = Modifier.testTag("player_rewind_button")
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Rewind 5s")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    FilledIconButton(
                        onClick = {
                            videoViewRef?.let { vView ->
                                if (vView.isPlaying) {
                                    vView.pause()
                                    isPlaying = false
                                } else {
                                    if (currentPositionMs >= totalDurationMs - 300) {
                                        vView.seekTo(0)
                                        currentPositionMs = 0L
                                    }
                                    vView.start()
                                    isPlaying = true
                                }
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = RecorderRed
                        ),
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = {
                            val target = (currentPositionMs + 5000L).coerceAtMost(totalDurationMs)
                            videoViewRef?.seekTo(target.toInt())
                            currentPositionMs = target
                        },
                        modifier = Modifier.testTag("player_forward_button")
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Forward 5s")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Video Details Card
                val sizeMb = recording.fileSizeBytes.toDouble() / (1024.0 * 1024.0)
                val formattedSize = String.format(Locale.getDefault(), "%.1f MB", sizeMb.coerceAtLeast(0.1))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Technical Specifications",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Resolution: ${recording.resolution}", style = MaterialTheme.typography.bodySmall)
                            Text("Container: ${recording.format}", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bitrate: ${recording.bitrateMbps} Mbps", style = MaterialTheme.typography.bodySmall)
                            Text("Frame Rate: ${recording.fps} FPS", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Audio: ${recording.audioSource}", style = MaterialTheme.typography.bodySmall)
                            Text("Size: $formattedSize", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDownload != null) {
                        AssistChip(
                            onClick = { onDownload(recording) },
                            enabled = !isDownloading,
                            label = { Text(if (isDownloading) "Saving..." else "Save to Phone") },
                            leadingIcon = {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = CyberCyan
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        tint = CyberCyan
                                    )
                                }
                            },
                            modifier = Modifier.testTag("player_save_to_phone_button")
                        )
                    }

                    AssistChip(
                        onClick = {
                            try { videoViewRef?.stopPlayback() } catch (_: Exception) {}
                            onDismiss()
                            onOpenTrimmer(recording)
                        },
                        label = { Text("Trim Clip") },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCut, contentDescription = null, tint = RecorderRed)
                        },
                        modifier = Modifier.testTag("player_open_trimmer_button")
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSec = millis / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val ms = (millis % 1000) / 100
    return String.format(Locale.getDefault(), "%02d:%02d.%d", min, sec, ms)
}
