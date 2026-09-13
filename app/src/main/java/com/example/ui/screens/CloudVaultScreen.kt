package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.CloudConfig
import com.example.data.model.RecordingEntity
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RecorderRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudVaultScreen(
    isPremiumUser: Boolean,
    recordings: List<RecordingEntity>,
    useEncryption: Boolean,
    onToggleEncryption: (Boolean) -> Unit,
    onUpgradeClick: () -> Unit,
    onRestore: (RecordingEntity) -> Unit,
    isCloudStorageLocked: Boolean = CloudConfig.IS_CLOUD_STORAGE_LOCKED,
    modifier: Modifier = Modifier
) {
    val cloudRecordings = recordings.filter { it.isCloudBackedUp }
    val cloudBytes = cloudRecordings.sumOf { it.fileSizeBytes }
    val cloudGbUsed = Math.round((cloudBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)) * 10.0) / 10.0
    val cloudLimitGb = if (isPremiumUser) 100.0 else 0.0

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("cloud_vault_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Locked Announcement Banner
        if (isCloudStorageLocked) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cloud_locked_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF261818)
                    ),
                    border = BorderStroke(1.5.dp, AmberWarning)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AmberWarning.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Temporarily Locked",
                                    tint = AmberWarning,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = CloudConfig.LOCK_TITLE,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarning
                                )
                                Text(
                                    text = "Pricing Maintenance Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberWarning.copy(alpha = 0.25f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "LOCKED",
                                    color = AmberWarning,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = CloudConfig.LOCK_MESSAGE,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Local recordings are safe on your device. Cloud sync will resume once pricing is resolved.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cloud Vault",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isCloudStorageLocked) "Cloud features temporarily locked for all users" else "Encrypted offsite storage to free device space",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCloudStorageLocked) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCloudStorageLocked) AmberWarning.copy(alpha = 0.25f)
                            else if (isPremiumUser) AmberWarning.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isCloudStorageLocked) Icons.Default.Lock else if (isPremiumUser) Icons.Default.Star else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isCloudStorageLocked || isPremiumUser) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCloudStorageLocked) "LOCKED" else if (isPremiumUser) "PRO VAULT" else "FREE TIER",
                            color = if (isCloudStorageLocked || isPremiumUser) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Storage Quota Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (isCloudStorageLocked) {
                    BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f))
                } else if (isPremiumUser) {
                    CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(AmberWarning, CyberCyan)))
                } else null
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (isCloudStorageLocked) AmberWarning else if (isPremiumUser) CyberCyan else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isCloudStorageLocked) "Cloud Storage (Locked)"
                                        else if (isPremiumUser) "Cloud Storage (100 GB Plan)"
                                        else "Local Only Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isCloudStorageLocked) "Locked due to pricing maintenance for all users"
                                        else if (isPremiumUser) "$cloudGbUsed GB used of 100 GB"
                                        else "Cloud backup locked to Premium Prize",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCloudStorageLocked) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (isCloudStorageLocked) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { /* Locked during price fixing */ },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth().testTag("vault_unlock_banner_button"),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cloud Storage Temporarily Locked", fontWeight = FontWeight.Bold)
                        }
                    } else if (isPremiumUser) {
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { (cloudGbUsed / 100.0).toFloat().coerceIn(0.01f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = CyberCyan
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onUpgradeClick,
                            modifier = Modifier.fillMaxWidth().testTag("vault_unlock_banner_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RecorderRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock Cloud Storage Prize ($4.99)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Encryption Security Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("AES-256-GCM Encryption", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Zero-knowledge privacy for recordings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = useEncryption,
                            onCheckedChange = onToggleEncryption,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CyberCyan),
                            modifier = Modifier.testTag("encryption_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "When enabled, video streams are encrypted locally on your phone before uploading to external servers. Your secret key remains safely stored in hardware keystore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Cloud Recordings Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Backed Up Videos (${cloudRecordings.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${cloudRecordings.size} files in vault",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (cloudRecordings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(42.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Cloud Backups Yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Tap 'Cloud Vault' on any clip in the Recordings tab to encrypt and back it up.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        } else {
            items(cloudRecordings, key = { "cloud_${it.id}" }) { item ->
                CloudRecordingItem(
                    recording = item,
                    onRestore = { onRestore(item) }
                )
            }
        }
    }
}

@Composable
private fun CloudRecordingItem(
    recording: RecordingEntity,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (recording.isEncrypted) Color(0xFF004D40) else MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (recording.isEncrypted) Icons.Default.Lock else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (recording.isEncrypted) CyberCyan else NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${recording.format} • ${recording.fileSizeBytes / (1024 * 1024)} MB • ${if (recording.isEncrypted) "AES-256 Encrypted" else "Standard"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onRestore,
                modifier = Modifier.testTag("restore_cloud_clip_${recording.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Restore clip",
                    tint = CyberCyan
                )
            }
        }
    }
}
