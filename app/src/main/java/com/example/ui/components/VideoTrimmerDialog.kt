package com.example.ui.components

import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

@Composable
fun VideoTrimmerDialog(
    recording: RecordingEntity,
    onDismiss: () -> Unit,
    onSaveTrimmed: (title: String, startMs: Long, endMs: Long) -> Unit
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var totalDurationMs by remember { mutableLongStateOf(recording.durationMs.coerceAtLeast(1000L)) }
    var sliderRange by remember { mutableStateOf(0f..1f) }
    var clipTitle by remember { mutableStateOf("${recording.title} (Trimmed)") }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var isVideoPrepared by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    val videoFile = remember(recording.filePath) { File(recording.filePath) }

    val startMs = (sliderRange.start * totalDurationMs).toLong()
    val endMs = (sliderRange.endInclusive * totalDurationMs).toLong().coerceAtLeast(startMs + 500L)
    val trimmedDurationMs = (endMs - startMs).coerceAtLeast(500L)

    // Loop preview playback within startMs..endMs
    LaunchedEffect(isPreviewPlaying, sliderRange, totalDurationMs) {
        if (isPreviewPlaying && videoViewRef != null) {
            val vView = videoViewRef!!
            vView.seekTo(startMs.toInt())
            vView.start()

            while (isPreviewPlaying) {
                delay(40)
                if (vView.currentPosition >= endMs || !vView.isPlaying) {
                    vView.seekTo(startMs.toInt())
                    vView.start()
                }
            }
        } else {
            videoViewRef?.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {}
        }
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
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            tint = RecorderRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Video Trimmer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            try { videoViewRef?.stopPlayback() } catch (_: Exception) {}
                            onDismiss()
                        },
                        modifier = Modifier.testTag("trimmer_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close trimmer")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Video Preview Area with Native VideoView Playback
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoFile.exists() && !hasError) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("trimmer_native_video_view"),
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
                                        seekTo(startMs.toInt())
                                    }
                                    setOnCompletionListener {
                                        isPreviewPlaying = false
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        hasError = true
                                        true
                                    }
                                }
                            }
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = RecorderRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Unable to load video stream",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Floating play/pause button overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable {
                                isPreviewPlaying = !isPreviewPlaying
                            }
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPreviewPlaying) "Pause Preview" else "Play Preview",
                            tint = if (isPreviewPlaying) CyberCyan else RecorderRed,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Top indicator for format preservation
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isPreviewPlaying) "Previewing Selection" else "Exporting as ${recording.format}",
                            color = if (isPreviewPlaying) RecorderRed else CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline Strip & Handles
                Text(
                    text = "Trim Range",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Visual Timeline Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    val widthFraction = (sliderRange.endInclusive - sliderRange.start).coerceIn(0.02f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(widthFraction)
                                .height(32.dp)
                                .align(Alignment.CenterStart)
                                .background(RecorderRed.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                .border(1.5.dp, RecorderRed, RoundedCornerShape(6.dp))
                        )
                    }
                }

                // Range slider for adjusting trim
                RangeSlider(
                    value = sliderRange,
                    onValueChange = { range ->
                        sliderRange = range
                        isPreviewPlaying = false
                        val targetMs = (range.start * totalDurationMs).toInt()
                        videoViewRef?.seekTo(targetMs)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trimmer_range_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = RecorderRed,
                        activeTrackColor = RecorderRed,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Time Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Start Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatTimePrecise(startMs), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = RecorderRed)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Trimmed Duration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatTimePrecise(trimmedDurationMs), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("End Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatTimePrecise(endMs), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = RecorderRed)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Clip Name input
                OutlinedTextField(
                    value = clipTitle,
                    onValueChange = { clipTitle = it },
                    label = { Text("Trimmed Video Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trimmer_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            sliderRange = 0f..1f
                            isPreviewPlaying = false
                            videoViewRef?.seekTo(0)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trimmer_reset_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset")
                    }

                    Button(
                        onClick = {
                            try { videoViewRef?.stopPlayback() } catch (_: Exception) {}
                            onSaveTrimmed(clipTitle.ifBlank { "${recording.title} (Trimmed)" }, startMs, endMs)
                        },
                        modifier = Modifier
                            .weight(2f)
                            .testTag("trimmer_save_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RecorderRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Trimmed Clip", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatTimePrecise(millis: Long): String {
    val totalSec = millis / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val frac = (millis % 1000) / 100
    return String.format("%02d:%02d.%d", min, sec, frac)
}
