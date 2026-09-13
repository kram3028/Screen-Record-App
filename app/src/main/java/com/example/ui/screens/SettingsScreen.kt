package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioSourceMode
import com.example.data.model.ProFeatureManager
import com.example.data.model.RecorderConfig
import com.example.data.model.VideoFormat
import com.example.data.model.VideoResolution
import com.example.service.FloatingControlOverlay
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RecorderRed

@Composable
fun SettingsScreen(
    config: RecorderConfig,
    onConfigChange: (RecorderConfig) -> Unit,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    isProUnlocked: Boolean = false,
    onRequestUnlockPro: (featureName: String) -> Unit = {},
    onRequestOverlayPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item {
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Encoder resolution, dual-audio sources & floating controls",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Appearance Section (Dark / Light mode)
        item {
            SettingsSectionHeader(icon = Icons.Default.Palette, title = "Appearance & Theme")
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (isDarkTheme) "High-contrast obsidian theme active" else "Bright daylight theme active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleDarkTheme() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RecorderRed),
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
        }

        // Video Settings Section
        item {
            SettingsSectionHeader(icon = Icons.Default.Videocam, title = "Video Encoder Settings")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Resolution
                    Text("Export Resolution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    VideoResolution.values().forEach { res ->
                        val isLocked = ProFeatureManager.isProResolution(res) && !isProUnlocked
                        RadioOptionRow(
                            title = res.label,
                            subtitle = res.description,
                            selected = config.resolution == res,
                            isLocked = isLocked,
                            onClick = {
                                if (isLocked) {
                                    onRequestUnlockPro(res.label)
                                } else {
                                    onConfigChange(config.copy(resolution = res))
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Video Container Format (MP4 vs MKV)
                    Text("Container Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    VideoFormat.values().forEach { format ->
                        RadioOptionRow(
                            title = format.label,
                            subtitle = if (format == VideoFormat.MP4) "Best for social media and editing software" else "Resilient to crashes and power loss",
                            selected = config.format == format,
                            onClick = { onConfigChange(config.copy(format = format)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Frame Rate
                    Text("Frame Rate (FPS)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 60, 120).forEach { fps ->
                            val isLocked = ProFeatureManager.isProFps(fps) && !isProUnlocked
                            SelectableChip(
                                text = "$fps FPS",
                                isSelected = config.fps == fps,
                                isLocked = isLocked,
                                onClick = {
                                    if (isLocked) {
                                        onRequestUnlockPro("$fps FPS")
                                    } else {
                                        onConfigChange(config.copy(fps = fps))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Video Bitrate
                    Text("Video Bitrate", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(4, 8, 16, 24).forEach { mbps ->
                            val isLocked = ProFeatureManager.isProBitrate(mbps) && !isProUnlocked
                            SelectableChip(
                                text = "$mbps Mbps",
                                isSelected = config.bitrateMbps == mbps,
                                isLocked = isLocked,
                                onClick = {
                                    if (isLocked) {
                                        onRequestUnlockPro("$mbps Mbps Bitrate")
                                    } else {
                                        onConfigChange(config.copy(bitrateMbps = mbps))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Audio Settings Section
        item {
            SettingsSectionHeader(icon = Icons.Default.Audiotrack, title = "Audio Capture")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Audio Input Source", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    AudioSourceMode.values().forEach { source ->
                        RadioOptionRow(
                            title = source.label,
                            subtitle = source.description,
                            selected = config.audioSource == source,
                            onClick = { onConfigChange(config.copy(audioSource = source)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Audio Bitrate", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(128, 192, 256, 320).forEach { kbps ->
                            SelectableChip(
                                text = "$kbps k",
                                isSelected = config.audioBitrateKbps == kbps,
                                onClick = { onConfigChange(config.copy(audioBitrateKbps = kbps)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Controls & Floating Widget Section
        item {
            SettingsSectionHeader(icon = Icons.Default.Widgets, title = "Overlay & Accessibility")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Floating Ball
                    val context = LocalContext.current
                    ToggleRow(
                        title = "Floating Control Bubble",
                        subtitle = "Show draggable bubble on screen during recording",
                        checked = config.showFloatingControls,
                        onCheckedChange = { enabled ->
                            if (enabled && !FloatingControlOverlay.canDrawOverlay(context)) {
                                onRequestOverlayPermission()
                            }
                            onConfigChange(config.copy(showFloatingControls = enabled))
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Countdown seconds
                    Text("Countdown Timer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 3, 5, 10).forEach { sec ->
                            SelectableChip(
                                text = if (sec == 0) "None" else "${sec}s",
                                isSelected = config.countdownSeconds == sec,
                                onClick = { onConfigChange(config.copy(countdownSeconds = sec)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stop on Screen Off
                    ToggleRow(
                        title = "Stop on Screen Turn-Off",
                        subtitle = "Automatically finalize and save video when screen locks",
                        checked = config.stopOnScreenOff,
                        onCheckedChange = { onConfigChange(config.copy(stopOnScreenOff = it)) }
                    )
                }
            }
        }

        // About & Version Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = RecorderRed, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Screen Recorder Pro", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Version 3.2.0 • Material You M3", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Full-screen HD video capture with dual-stream audio & AES-256 Cloud Vault.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = RecorderRed, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RadioOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    isLocked: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = RecorderRed)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (isLocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmberWarning.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "AD UNLOCK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                        }
                    }
                }
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RecorderRed)
        )
    }
}

@Composable
private fun SelectableChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) RecorderRed
                else if (isLocked) AmberWarning.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (isSelected) Color.Transparent
                else if (isLocked) AmberWarning.copy(alpha = 0.4f)
                else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            if (isLocked && !isSelected) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else if (isLocked) AmberWarning else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
