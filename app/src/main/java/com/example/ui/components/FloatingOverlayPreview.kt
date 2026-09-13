package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RecorderRed

@Composable
fun FloatingOverlayPreviewCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    isRecording: Boolean,
    isPaused: Boolean,
    onRecordAction: () -> Unit,
    onPauseAction: () -> Unit,
    onStopAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Floating Control Overlay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Quick on-screen bubble to control recording over any app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RecorderRed
                    ),
                    modifier = Modifier.testTag("floating_overlay_toggle")
                )
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.size(14.dp))

                // Interactive Bubble Widget Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Floating Bubble (Draggable handle simulation)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(RecorderRed)
                                .clickable { isExpanded = !isExpanded }
                                .testTag("floating_bubble_trigger"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.FiberManualRecord,
                                contentDescription = "Floating Bubble",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Expanded toolbar buttons
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFF1E212B))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isRecording) {
                                    BubbleActionItem(
                                        icon = Icons.Default.FiberManualRecord,
                                        tint = RecorderRed,
                                        label = "Start",
                                        onClick = onRecordAction
                                    )
                                } else {
                                    BubbleActionItem(
                                        icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        tint = CyberCyan,
                                        label = if (isPaused) "Resume" else "Pause",
                                        onClick = onPauseAction
                                    )
                                    BubbleActionItem(
                                        icon = Icons.Default.Stop,
                                        tint = Color.White,
                                        label = "Stop",
                                        onClick = onStopAction
                                    )
                                }

                                BubbleActionItem(
                                    icon = Icons.Default.CameraAlt,
                                    tint = ElectricBlue,
                                    label = "Snap",
                                    onClick = {}
                                )
                                BubbleActionItem(
                                    icon = Icons.Default.Brush,
                                    tint = NeonGreen,
                                    label = "Draw",
                                    onClick = {}
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.size(6.dp))

                Text(
                    text = if (isExpanded) "Tap the red button to collapse overlay controls" else "Tap preview circle to expand full control bar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun BubbleActionItem(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}
