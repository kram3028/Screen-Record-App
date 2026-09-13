package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.config.CloudConfig
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RecorderRed

@Composable
fun CloudUpgradeDialog(
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit,
    onWatchAdToUnlock: () -> Unit = {},
    isLocked: Boolean = CloudConfig.IS_CLOUD_STORAGE_LOCKED
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberWarning.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isLocked) Icons.Default.Lock else Icons.Default.Star, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isLocked) "CLOUD VAULT (LOCKED)" else "PREMIUM CLOUD VAULT", color = AmberWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cloud_upgrade_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hero Cloud Badge
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isLocked) listOf(Color(0xFF5C2D2D), Color(0xFF7A3E26))
                                else listOf(RecorderRed, Color(0xFFFF8E53))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isLocked) "Cloud Storage Temporarily Locked" else "Upgrade to Cloud Vault Pro",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isLocked) "Purchases and cloud vaults are temporarily paused while price fixing is underway" else "Free up device storage with encrypted cloud backups",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLocked) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (isLocked) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1A1A)),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Notice: Cloud purchases are temporarily locked for all users due to pricing adjustments. We will reopen as soon as the pricing fix is finalized.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Feature Highlights
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureRow(
                        icon = Icons.Default.Storage,
                        title = "100 GB Cloud Storage",
                        subtitle = "Save phone storage by offloading heavy 1080p & 2K captures"
                    )
                    FeatureRow(
                        icon = Icons.Default.Lock,
                        title = "Military-Grade AES-256 Encryption",
                        subtitle = "Client-side zero-knowledge security for confidential videos"
                    )
                    FeatureRow(
                        icon = Icons.Default.Security,
                        title = "Instant Restore & Sync",
                        subtitle = "Stream or restore your original recordings anytime anywhere"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Pricing Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(RecorderRed, CyberCyan)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Lifetime Cloud Prize",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isLocked) "Pricing fix currently in progress" else "One-time payment • No recurring bill",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isLocked) "LOCKED" else "$4.99",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLocked) AmberWarning else RecorderRed
                            )
                            Text(
                                text = if (isLocked) "MAINTENANCE" else "PRO ACCESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLocked) AmberWarning else CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Purchase CTA
                Button(
                    onClick = { if (!isLocked) onPurchaseSuccess() },
                    enabled = !isLocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("cloud_purchase_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RecorderRed,
                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(if (isLocked) Icons.Default.Lock else Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLocked) "Temporarily Locked (Price Maintenance)" else "Claim Cloud Storage Prize ($4.99)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { if (!isLocked) onWatchAdToUnlock() },
                    enabled = !isLocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cloud_watch_ad_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyberCyan,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.5.dp, if (isLocked) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else CyberCyan)
                ) {
                    Icon(if (isLocked) Icons.Default.Lock else Icons.Default.PlayCircle, contentDescription = null, tint = if (isLocked) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLocked) "Unlock Paused During Maintenance" else "Watch Ad to Unlock Free (Rewarded)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(RecorderRed.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RecorderRed,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
