package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioSourceMode
import com.example.data.model.LiveRecordingMetrics
import com.example.data.model.ProFeatureManager
import com.example.data.model.RecorderConfig
import com.example.data.model.RecordingState
import com.example.data.model.VideoFormat
import com.example.data.model.VideoResolution
import com.example.monitor.SystemResourceStats
import com.example.service.FloatingControlOverlay
import com.example.ui.components.FloatingOverlayPreviewCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RecorderRed

@Composable
fun RecorderScreen(
    recordingState: RecordingState,
    countdownValue: Int,
    liveMetrics: LiveRecordingMetrics,
    config: RecorderConfig,
    systemStats: SystemResourceStats,
    onStartRecording: () -> Unit,
    onCancelCountdown: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onConfigChange: (RecorderConfig) -> Unit,
    onNavigateToMonitor: () -> Unit,
    isProUnlocked: Boolean = false,
    onRequestUnlockPro: (featureName: String) -> Unit = {},
    onRequestOverlayPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRecording = recordingState == RecordingState.RECORDING
    val isPaused = recordingState == RecordingState.PAUSED
    val isCountdown = countdownValue > 0

    // Pulse animation for recording ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.14f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header / Status Banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Screen Studio",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isRecording) "Recording Full Screen in ${config.resolution.label}" else "Ready to capture screen & audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Active State Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isRecording) RecorderRed.copy(alpha = 0.15f)
                                else if (isPaused) ElectricBlue.copy(alpha = 0.15f)
                                else NeonGreen.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (isRecording) RecorderRed else if (isPaused) ElectricBlue else NeonGreen,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) RecorderRed else if (isPaused) ElectricBlue else NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRecording) "REC ON" else if (isPaused) "PAUSED" else "READY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) RecorderRed else if (isPaused) ElectricBlue else NeonGreen
                            )
                        }
                    }
                }
            }

            // Big Circular Record Trigger & Live HUD
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer pulsing halo
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(RecorderRed.copy(alpha = 0.18f))
                        )
                    }

                    // Middle boundary ring
                    Box(
                        modifier = Modifier
                            .size(175.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 3.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        if (isRecording) RecorderRed else MaterialTheme.colorScheme.outline,
                                        if (isRecording) Color(0xFFFF8E53) else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner Main Record Button
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            if (isRecording) RecorderRed else RecorderRed,
                                            if (isRecording) Color(0xFFC41235) else Color(0xFFD61339)
                                        )
                                    )
                                )
                                .clickable {
                                    if (isRecording || isPaused) {
                                        onStopRecording()
                                    } else if (!isCountdown) {
                                        onStartRecording()
                                    }
                                }
                                .testTag("main_record_action_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRecording || isPaused) {
                                // Stop Square
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                )
                            } else {
                                // Record Camera Aperture
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Start Recording",
                                        tint = Color.White,
                                        modifier = Modifier.size(46.dp)
                                    )
                                    Text(
                                        text = "RECORD",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real-Time Recording Telemetry HUD (Visible when Recording or Paused)
            item {
                AnimatedVisibility(
                    visible = isRecording || isPaused,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(RecorderRed, CyberCyan)))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Big Live Timer
                            Text(
                                text = formatLiveTime(liveMetrics.elapsedMs),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPaused) ElectricBlue else RecorderRed
                            )

                            Text(
                                text = if (isPaused) "Recording is Paused" else "High-Bitrate Capture in Progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 3 Live Metric Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                LiveMetricItem(
                                    label = "Bitrate",
                                    value = "${liveMetrics.currentBitrateMbps} Mbps",
                                    tint = CyberCyan
                                )
                                LiveMetricItem(
                                    label = "Size",
                                    value = formatTelemetrySize(liveMetrics.fileSizeBytes),
                                    tint = Color.White
                                )
                                LiveMetricItem(
                                    label = "FPS",
                                    value = "${liveMetrics.fps} fps",
                                    tint = NeonGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Action Buttons (Pause / Resume and Stop)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (isPaused) onResumeRecording() else onPauseRecording()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("session_pause_resume_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isPaused) "Resume" else "Pause")
                                }

                                Button(
                                    onClick = onStopRecording,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("session_stop_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = RecorderRed),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stop & Save", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Configuration Bar (Resolution, Format, FPS, Audio Source)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Quick Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row of interactive configuration chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Resolution Pill
                        val isResLocked = ProFeatureManager.isProResolution(config.resolution) && !isProUnlocked
                        QuickConfigPill(
                            label = config.resolution.label,
                            isSelected = true,
                            isLocked = isResLocked,
                            onClick = {
                                val nextRes = when (config.resolution) {
                                    VideoResolution.RES_720P -> VideoResolution.RES_1080P
                                    VideoResolution.RES_1080P -> VideoResolution.RES_2K
                                    VideoResolution.RES_2K -> VideoResolution.RES_720P
                                }
                                if (ProFeatureManager.isProResolution(nextRes) && !isProUnlocked) {
                                    onRequestUnlockPro(nextRes.label)
                                } else {
                                    onConfigChange(config.copy(resolution = nextRes))
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("quick_res_pill")
                        )

                        // Format Pill (MP4 / MKV)
                        QuickConfigPill(
                            label = config.format.name,
                            isSelected = true,
                            onClick = {
                                val nextFormat = if (config.format == VideoFormat.MP4) VideoFormat.MKV else VideoFormat.MP4
                                onConfigChange(config.copy(format = nextFormat))
                            },
                            modifier = Modifier.weight(1f).testTag("quick_format_pill")
                        )

                        // FPS Pill
                        val isFpsLocked = ProFeatureManager.isProFps(config.fps) && !isProUnlocked
                        QuickConfigPill(
                            label = "${config.fps} FPS",
                            isSelected = true,
                            isLocked = isFpsLocked,
                            onClick = {
                                val nextFps = when (config.fps) {
                                    30 -> 60
                                    60 -> 120
                                    else -> 30
                                }
                                if (ProFeatureManager.isProFps(nextFps) && !isProUnlocked) {
                                    onRequestUnlockPro("$nextFps FPS")
                                } else {
                                    onConfigChange(config.copy(fps = nextFps))
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("quick_fps_pill")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Audio Source Quick Selector
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val nextSource = when (config.audioSource) {
                                    AudioSourceMode.MIC_AND_SYSTEM -> AudioSourceMode.MIC_ONLY
                                    AudioSourceMode.MIC_ONLY -> AudioSourceMode.SYSTEM_ONLY
                                    AudioSourceMode.SYSTEM_ONLY -> AudioSourceMode.MUTE
                                    AudioSourceMode.MUTE -> AudioSourceMode.MIC_AND_SYSTEM
                                }
                                onConfigChange(config.copy(audioSource = nextSource))
                            }
                            .testTag("audio_source_quick_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Audio Input",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = config.audioSource.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "Tap to Switch",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Real-Time System Resources Quick Glance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMonitor() }
                        .testTag("system_glance_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = RecorderRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "System Resources Monitor",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "View All →",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("CPU Load", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${systemStats.cpuUsagePercent}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            }
                            Column {
                                Text("RAM Used", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${systemStats.ramUsedMb} MB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            }
                            Column {
                                Text("Battery Temp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${systemStats.batteryTemperatureCelsius}°C", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            }
                            Column {
                                Text("Free Disk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${systemStats.storageFreeGb} GB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = NeonGreen)
                            }
                        }
                    }
                }
            }

            // Floating Overlay Card
            item {
                val context = LocalContext.current
                FloatingOverlayPreviewCard(
                    isEnabled = config.showFloatingControls,
                    onToggle = { enabled ->
                        if (enabled && !FloatingControlOverlay.canDrawOverlay(context)) {
                            onRequestOverlayPermission()
                        }
                        onConfigChange(config.copy(showFloatingControls = enabled))
                    },
                    isRecording = isRecording,
                    isPaused = isPaused,
                    onRecordAction = onStartRecording,
                    onPauseAction = onPauseRecording,
                    onStopAction = onStopRecording
                )
            }
        }

        // Full Screen Countdown Overlay
        if (isCountdown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Recording starts in",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "$countdownValue",
                        color = RecorderRed,
                        fontSize = 110.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = onCancelCountdown,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3342)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("cancel_countdown_button")
                    ) {
                        Text("Cancel Countdown", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickConfigPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = if (isLocked) {
            CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(AmberWarning, AmberWarning.copy(alpha = 0.5f))))
        } else {
            CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(RecorderRed.copy(alpha = 0.5f), CyberCyan.copy(alpha = 0.5f))))
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isLocked) AmberWarning else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LiveMetricItem(
    label: String,
    value: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tint
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTelemetrySize(bytes: Long): String {
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb < 0.1) {
        val kb = bytes / 1024
        "${kb.coerceAtLeast(1)} KB"
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
    }
}

private fun formatLiveTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
