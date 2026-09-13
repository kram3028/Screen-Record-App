package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.CloudConfig
import com.example.data.model.RecordingEntity
import com.example.ui.components.AdmobNativeCard
import com.example.ui.components.RenameRecordingDialog
import com.example.ui.components.VideoThumbnailView
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RecorderRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GalleryViewMode {
    GRID,
    LIST
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordingsScreen(
    recordings: List<RecordingEntity>,
    onPlay: (RecordingEntity) -> Unit,
    onTrim: (RecordingEntity) -> Unit,
    onBackup: (RecordingEntity) -> Unit,
    onDelete: (RecordingEntity) -> Unit,
    onShare: (RecordingEntity) -> Unit,
    onDownload: ((RecordingEntity) -> Unit)? = null,
    onRename: ((RecordingEntity, String) -> Unit)? = null,
    onDeleteMultiple: ((List<RecordingEntity>) -> Unit)? = null,
    downloadingRecordingId: Long? = null,
    cloudSyncInProgressId: Long?,
    cloudSyncProgress: Float,
    isCloudStorageLocked: Boolean = CloudConfig.IS_CLOUD_STORAGE_LOCKED,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, MP4, MKV, CLOUD
    var viewMode by remember { mutableStateOf(GalleryViewMode.GRID) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingToRename by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingForDetails by remember { mutableStateOf<RecordingEntity?>(null) }

    val filteredList = recordings.filter { item ->
        val matchesSearch = item.title.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "MP4" -> item.format.equals("MP4", ignoreCase = true)
            "MKV" -> item.format.equals("MKV", ignoreCase = true)
            "CLOUD" -> item.isCloudBackedUp
            else -> true
        }
        matchesSearch && matchesFilter
    }

    val totalStorageBytes = recordings.sumOf { it.fileSizeBytes }
    val totalStorageMb = totalStorageBytes / (1024 * 1024)
    val selectedStorageBytes = filteredList.filter { it.id in selectedIds }.sumOf { it.fileSizeBytes }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Selection Mode Header vs. Normal Search Header
            if (isSelectionMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, RecorderRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedIds = emptySet()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("exit_selection_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit selection mode",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = "${selectedIds.size} of ${filteredList.size} selected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (selectedIds.isNotEmpty()) {
                                    Text(
                                        text = "Freed: ${formatDisplayFileSize(selectedStorageBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RecorderRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = {
                                selectedIds = if (selectedIds.size == filteredList.size) {
                                    emptySet()
                                } else {
                                    filteredList.map { it.id }.toSet()
                                }
                            },
                            modifier = Modifier.testTag("select_all_toggle_button")
                        ) {
                            Text(
                                text = if (selectedIds.size == filteredList.size && filteredList.isNotEmpty()) "Deselect All" else "Select All",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    showDeleteSelectedDialog = true
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("delete_selected_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = RecorderRed,
                                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected recordings",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Search, View Toggle, Select Trigger Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recordings_search_input"),
                        placeholder = { Text("Search recordings...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Gallery Controls Surface: Grid, List, and Multi-Select triggers
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewMode = GalleryViewMode.GRID },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (viewMode == GalleryViewMode.GRID) RecorderRed.copy(alpha = 0.2f) else Color.Transparent
                                    )
                                    .testTag("view_mode_grid_button")
                            ) {
                                Icon(
                                    Icons.Default.GridView,
                                    contentDescription = "Grid View",
                                    tint = if (viewMode == GalleryViewMode.GRID) RecorderRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewMode = GalleryViewMode.LIST },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (viewMode == GalleryViewMode.LIST) RecorderRed.copy(alpha = 0.2f) else Color.Transparent
                                    )
                                    .testTag("view_mode_list_button")
                            ) {
                                Icon(
                                    Icons.Default.ViewList,
                                    contentDescription = "List View",
                                    tint = if (viewMode == GalleryViewMode.LIST) RecorderRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    isSelectionMode = true
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("enter_selection_mode_button")
                            ) {
                                Icon(
                                    Icons.Default.Checklist,
                                    contentDescription = "Select Videos",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${recordings.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RecorderRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_all")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "MP4",
                        onClick = { selectedFilter = "MP4" },
                        label = { Text("MP4 Formats") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RecorderRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_mp4")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "MKV",
                        onClick = { selectedFilter = "MKV" },
                        label = { Text("MKV Formats") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RecorderRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_mkv")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "CLOUD",
                        onClick = { selectedFilter = "CLOUD" },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Cloud Vault")
                                if (isCloudStorageLocked) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = AmberWarning,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RecorderRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_cloud")
                    )
                }
            }

            // Summary Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredList.size} recordings • ${formatDisplayFileSize(totalStorageBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (filteredList.isNotEmpty()) {
                    TextButton(
                        onClick = { isSelectionMode = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("bulk_delete_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = RecorderRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bulk Delete",
                            color = RecorderRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        }

        // Gallery Grid or List Content
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching recordings" else "No recordings found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try a different search keyword" else "Start recording your screen to see your videos in this gallery grid.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            when (viewMode) {
                GalleryViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 165.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("gallery_grid_view"),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            val isSelected = item.id in selectedIds
                            VideoGalleryGridCard(
                                recording = item,
                                onPlay = {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                    } else {
                                        onPlay(item)
                                    }
                                },
                                onShare = { onShare(item) },
                                onDelete = { recordingToDelete = item },
                                onTrim = { onTrim(item) },
                                onBackup = { onBackup(item) },
                                onRename = { recordingToRename = item },
                                onDownload = onDownload?.let { cb -> { cb(item) } },
                                onShowDetails = { recordingForDetails = item },
                                isSyncing = cloudSyncInProgressId == item.id,
                                isDownloading = downloadingRecordingId == item.id,
                                isCloudStorageLocked = isCloudStorageLocked,
                                syncProgress = cloudSyncProgress,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                },
                                onLongClick = {
                                    isSelectionMode = true
                                    selectedIds = selectedIds + item.id
                                }
                            )
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            AdmobNativeCard(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
                GalleryViewMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("gallery_list_view"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            val isSelected = item.id in selectedIds
                            VideoGalleryListCard(
                                recording = item,
                                onPlay = {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                    } else {
                                        onPlay(item)
                                    }
                                },
                                onShare = { onShare(item) },
                                onDelete = { recordingToDelete = item },
                                onTrim = { onTrim(item) },
                                onBackup = { onBackup(item) },
                                onRename = { recordingToRename = item },
                                onDownload = onDownload?.let { cb -> { cb(item) } },
                                onShowDetails = { recordingForDetails = item },
                                isSyncing = cloudSyncInProgressId == item.id,
                                isDownloading = downloadingRecordingId == item.id,
                                isCloudStorageLocked = isCloudStorageLocked,
                                syncProgress = cloudSyncProgress,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                },
                                onLongClick = {
                                    isSelectionMode = true
                                    selectedIds = selectedIds + item.id
                                }
                            )
                        }
                        item {
                            AdmobNativeCard()
                        }
                    }
                }
            }
        }

    // Batch Delete Confirmation Dialog
    if (showDeleteSelectedDialog) {
        val selectedItems = recordings.filter { it.id in selectedIds }
        val selectedBytes = selectedItems.sumOf { it.fileSizeBytes }
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = RecorderRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete ${selectedItems.size} Video${if (selectedItems.size > 1) "s" else ""}?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to permanently delete the ${selectedItems.size} selected video recording${if (selectedItems.size > 1) "s" else ""}? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Total space to be freed: ${formatDisplayFileSize(selectedBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (selectedItems.isNotEmpty()) {
                                Text(
                                    text = "Selected videos:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                selectedItems.take(4).forEach { item ->
                                    Text(
                                        text = "• ${item.title} (${formatDisplayFileSize(item.fileSizeBytes)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (selectedItems.size > 4) {
                                    Text(
                                        text = "...and ${selectedItems.size - 4} more",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onDeleteMultiple != null) {
                            onDeleteMultiple(selectedItems)
                        } else {
                            selectedItems.forEach { onDelete(it) }
                        }
                        showDeleteSelectedDialog = false
                        isSelectionMode = false
                        selectedIds = emptySet()
                    },
                    modifier = Modifier.testTag("confirm_delete_selected_button")
                ) {
                    Text(
                        "Delete (${selectedItems.size})",
                        color = RecorderRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Video Dialog
    recordingToRename?.let { target ->
        RenameRecordingDialog(
            recording = target,
            onDismiss = { recordingToRename = null },
            onConfirmRename = { newTitle ->
                onRename?.invoke(target, newTitle)
                recordingToRename = null
            }
        )
    }

    // Delete Confirmation Dialog
    recordingToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("Delete Recording?") },
            text = {
                Column {
                    Text("Are you sure you want to permanently delete:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = target.title,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${target.resolution} • ${formatDisplayFileSize(target.fileSizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(target)
                        recordingToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", color = RecorderRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Video Properties / Info Dialog
    recordingForDetails?.let { target ->
        AlertDialog(
            onDismissRequest = { recordingForDetails = null },
            title = { Text("Video Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = target.title, fontWeight = FontWeight.Bold)
                    val dateFormatted = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(target.createdAt))
                    Text(text = "Created: $dateFormatted", fontSize = 12.sp)
                    Text(text = "Resolution: ${target.resolution}", fontSize = 12.sp)
                    Text(text = "Format: ${target.format}", fontSize = 12.sp)
                    Text(text = "Frame Rate: ${target.fps} FPS", fontSize = 12.sp)
                    Text(text = "Target Bitrate: ${target.bitrateMbps} Mbps", fontSize = 12.sp)
                    Text(text = "Audio Source: ${target.audioSource}", fontSize = 12.sp)
                    Text(text = "File Size: ${formatDisplayFileSize(target.fileSizeBytes)}", fontSize = 12.sp)
                    Text(text = "Storage Path: ${target.filePath}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { recordingForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
    }

    // Floating Bottom Bulk Delete Action Bar
    AnimatedVisibility(
        visible = isSelectionMode && selectedIds.isNotEmpty(),
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, RecorderRed.copy(alpha = 0.6f)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${selectedIds.size} video${if (selectedIds.size > 1) "s" else ""} selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Freed: ${formatDisplayFileSize(selectedStorageBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            selectedIds = if (selectedIds.size == filteredList.size) emptySet() else filteredList.map { it.id }.toSet()
                        },
                        modifier = Modifier.testTag("floating_bulk_select_all_button")
                    ) {
                        Text(
                            text = if (selectedIds.size == filteredList.size) "Deselect" else "Select All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { showDeleteSelectedDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RecorderRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("floating_bulk_delete_button")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Delete (${selectedIds.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
}

/**
 * Gallery Grid Card: Visual card with video thumbnail, quick-access playback,
 * native sharing, deletion, and overflow tools.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoGalleryGridCard(
    recording: RecordingEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onTrim: () -> Unit,
    onBackup: () -> Unit,
    onRename: () -> Unit,
    onDownload: (() -> Unit)? = null,
    onShowDetails: () -> Unit,
    isSyncing: Boolean,
    isDownloading: Boolean = false,
    isCloudStorageLocked: Boolean = false,
    syncProgress: Float,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onPlay()
                    }
                },
                onLongClick = {
                    onLongClick()
                }
            )
            .testTag("gallery_grid_card_${recording.id}"),
        shape = RoundedCornerShape(18.dp),
        border = if (isSelected) BorderStroke(2.dp, RecorderRed) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) RecorderRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Video Thumbnail Section (Clickable for instant playback or toggle selection)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.28f)
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelect()
                            } else {
                                onPlay()
                            }
                        },
                        onLongClick = {
                            onLongClick()
                        }
                    )
                    .testTag("gallery_thumbnail_click_${recording.id}")
            ) {
                VideoThumbnailView(
                    recording = recording,
                    modifier = Modifier.fillMaxSize(),
                    showPlayOverlay = !isSelectionMode,
                    showBadges = true
                )

                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) RecorderRed else Color.Black.copy(alpha = 0.65f))
                            .clickable { onToggleSelect() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isSelected) "Selected" else "Not selected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Metadata & Action Bar Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                val dateStr = SimpleDateFormat("MMM dd • HH:mm", Locale.getDefault()).format(Date(recording.createdAt))
                Text(
                    text = "$dateStr • ${formatDisplayFileSize(recording.fileSizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sync indicator if uploading
                if (isSyncing) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { syncProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = CyberCyan
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Bar: Play, Share, Delete, and More
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play Action
                    FilledIconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("play_button_${recording.id}"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = RecorderRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play recording",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Download to Phone (Rewarded Ad)
                    if (onDownload != null) {
                        IconButton(
                            onClick = onDownload,
                            enabled = !isDownloading,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("download_button_${recording.id}")
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberCyan
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Save to device",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Share Action
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("share_button_${recording.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share video",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete Action
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("delete_button_${recording.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete recording",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("more_menu_button_${recording.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Select / Bulk Delete") },
                                leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null, tint = RecorderRed, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onLongClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename Video") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = RecorderRed, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onRename()
                                }
                            )
                            if (onDownload != null) {
                                DropdownMenuItem(
                                    text = { Text("Save to Phone (Rewarded)") },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        onDownload()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Trim Video Clip") },
                                leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onTrim()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (isCloudStorageLocked) {
                                                if (recording.isCloudBackedUp) "Synced to Vault (Locked)" else "Backup to Vault (Locked)"
                                            } else {
                                                if (recording.isCloudBackedUp) "Synced to Vault" else "Backup to Vault"
                                            }
                                        )
                                        if (isCloudStorageLocked) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = AmberWarning,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isCloudStorageLocked) Icons.Default.Lock
                                        else if (recording.isCloudBackedUp) Icons.Default.CloudDone
                                        else Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = if (isCloudStorageLocked) AmberWarning
                                            else if (recording.isCloudBackedUp) NeonGreen
                                            else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onBackup()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Video Details") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onShowDetails()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Gallery List Card: Alternative row presentation for users who prefer standard list view.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoGalleryListCard(
    recording: RecordingEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onTrim: () -> Unit,
    onBackup: () -> Unit,
    onRename: () -> Unit,
    onDownload: (() -> Unit)? = null,
    onShowDetails: () -> Unit,
    isSyncing: Boolean,
    isDownloading: Boolean = false,
    isCloudStorageLocked: Boolean = false,
    syncProgress: Float,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onPlay()
                    }
                },
                onLongClick = {
                    onLongClick()
                }
            )
            .testTag("gallery_list_card_${recording.id}"),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, RecorderRed) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) RecorderRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) RecorderRed else Color.Black.copy(alpha = 0.5f))
                            .clickable { onToggleSelect() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isSelected) "Selected" else "Not selected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Video Preview Thumbnail Box
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 76.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    VideoThumbnailView(
                        recording = recording,
                        modifier = Modifier.fillMaxSize(),
                        showPlayOverlay = !isSelectionMode,
                        showBadges = true
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(recording.createdAt))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgeTag(text = recording.resolution)
                        BadgeTag(text = "${recording.fps} FPS")
                        BadgeTag(text = formatDisplayFileSize(recording.fileSizeBytes))
                    }
                }
            }

            if (isSyncing) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { syncProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = CyberCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledIconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("list_play_button_${recording.id}"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = RecorderRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play recording",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (onDownload != null) {
                        IconButton(
                            onClick = onDownload,
                            enabled = !isDownloading,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("list_download_button_${recording.id}")
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberCyan
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Save to device",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("list_share_button_${recording.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share video",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onRename,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("list_rename_button_${recording.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename video",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onTrim,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("list_trim_button_${recording.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Trim clip",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onBackup,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("list_backup_button_${recording.id}")
                    ) {
                        Icon(
                            imageVector = if (isCloudStorageLocked) Icons.Default.Lock
                                else if (recording.isCloudBackedUp) Icons.Default.CloudDone
                                else Icons.Default.CloudUpload,
                            contentDescription = if (isCloudStorageLocked) "Cloud Storage Locked" else "Backup to Vault",
                            tint = if (isCloudStorageLocked) AmberWarning
                                else if (recording.isCloudBackedUp) NeonGreen
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("list_delete_button_${recording.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete recording",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatDisplayFileSize(bytes: Long): String {
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb < 0.1) {
        val kb = bytes / 1024
        "${kb.coerceAtLeast(1)} KB"
    } else {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    }
}

@Composable
private fun BadgeTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
