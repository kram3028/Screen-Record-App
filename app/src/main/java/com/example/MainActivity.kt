package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdManager
import com.example.data.model.ProFeatureManager
import com.example.data.model.RecordingEntity
import com.example.data.model.RecordingState
import com.example.service.FloatingControlOverlay
import com.example.ui.components.AdmobBannerView
import com.example.ui.components.CloudUpgradeDialog
import com.example.ui.components.OverlayPermissionDialog
import com.example.ui.components.ProFeatureUnlockDialog
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.components.VideoTrimmerDialog
import com.example.ui.screens.CloudVaultScreen
import com.example.ui.screens.RecorderScreen
import com.example.ui.screens.RecordingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SystemMonitorScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RecorderRed
import com.example.ui.viewmodel.RecorderViewModel
import com.example.util.ShareUtils

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdManager.initialize(this)

        setContent {
            val recorderViewModel: RecorderViewModel = viewModel()
            val isDarkTheme by recorderViewModel.isDarkTheme.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                ScreenRecorderApp(viewModel = recorderViewModel)
            }
        }
    }
}

@Composable
fun ScreenRecorderApp(viewModel: RecorderViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val allRecordings by viewModel.allRecordings.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val countdownValue by viewModel.countdownValue.collectAsStateWithLifecycle()
    val liveMetrics by viewModel.liveMetrics.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val systemStats by viewModel.systemStats.collectAsStateWithLifecycle()
    val isPremiumUser by viewModel.isPremiumUser.collectAsStateWithLifecycle()
    val useEncryption by viewModel.useClientSideEncryption.collectAsStateWithLifecycle()
    val cloudSyncInProgressId by viewModel.cloudSyncInProgressId.collectAsStateWithLifecycle()
    val cloudSyncProgress by viewModel.cloudSyncProgress.collectAsStateWithLifecycle()
    val downloadingRecordingId by viewModel.downloadingRecordingId.collectAsStateWithLifecycle()
    val isCloudStorageLocked by viewModel.isCloudStorageLocked.collectAsStateWithLifecycle()
    val firebaseSyncState by viewModel.firebaseSyncState.collectAsStateWithLifecycle()

    val activePlayingRecording by viewModel.activePlayingRecording.collectAsStateWithLifecycle()
    val activeTrimmingRecording by viewModel.activeTrimmingRecording.collectAsStateWithLifecycle()
    val showPremiumDialog by viewModel.showPremiumDialog.collectAsStateWithLifecycle()
    val userFeedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isProUnlocked by viewModel.isProFeaturesUnlockedForSession.collectAsStateWithLifecycle()

    var showProUnlockDialog by remember { mutableStateOf(false) }
    var proUnlockFeaturesList by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingStartRecordAfterUnlock by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val handleDownloadWithRewardedAd: (RecordingEntity) -> Unit = { recording ->
        if (activity != null) {
            var rewardEarned = false
            if (AdManager.isRewardedReady.value) {
                AdManager.showRewarded(
                    activity = activity,
                    onRewardEarned = {
                        rewardEarned = true
                        viewModel.downloadRecordingToPhone(recording)
                    },
                    onDismissed = {
                        if (!rewardEarned) {
                            viewModel.showFeedback("Watch the rewarded ad to completion to save to device gallery!")
                        }
                    }
                )
            } else {
                AdManager.preloadRewarded(activity)
                viewModel.downloadRecordingToPhone(recording)
            }
        } else {
            viewModel.downloadRecordingToPhone(recording)
        }
    }

    // Media projection launcher
    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            viewModel.startRecordingFlow(result.resultCode, result.data)
        } else {
            // Permission rejected or canceled by user: start fallback demo session
            viewModel.startRecordingFlow(0, null)
        }
    }

    // Permission launcher for microphone and notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Start screen projection flow
        try {
            projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        } catch (e: Exception) {
            viewModel.startRecordingFlow(0, null)
        }
    }

    val onInitiateRecording: () -> Unit = {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            try {
                projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
            } catch (e: Exception) {
                viewModel.startRecordingFlow(0, null)
            }
        }
    }

    val requestUnlockPro: (String) -> Unit = { featureName ->
        proUnlockFeaturesList = if (ProFeatureManager.isProConfig(config)) {
            ProFeatureManager.getProFeaturesList(config)
        } else {
            listOf(featureName)
        }
        pendingStartRecordAfterUnlock = false
        showProUnlockDialog = true
    }

    val checkAndInitiateRecording: () -> Unit = {
        if (ProFeatureManager.isProConfig(config) && !isProUnlocked) {
            proUnlockFeaturesList = ProFeatureManager.getProFeaturesList(config)
            pendingStartRecordAfterUnlock = true
            showProUnlockDialog = true
        } else if (config.showFloatingControls && !FloatingControlOverlay.canDrawOverlay(context)) {
            showOverlayPermissionDialog = true
        } else {
            onInitiateRecording()
        }
    }

    // Handle feedback snackbars
    LaunchedEffect(userFeedbackMessage) {
        userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                // Banner is only shown on other screens (Videos, System, Cloud, Settings) - NOT on Home (Recorder)
                if (selectedTab != 0) {
                    AdmobBannerView()
                }
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                // Tab 0: Recorder
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = {
                        if (recordingState == RecordingState.RECORDING) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = RecorderRed)
                        } else {
                            Icon(Icons.Default.Videocam, contentDescription = null)
                        }
                    },
                    label = { Text("Recorder") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = RecorderRed.copy(alpha = 0.2f),
                        selectedIconColor = RecorderRed,
                        selectedTextColor = RecorderRed
                    ),
                    modifier = Modifier.testTag("nav_item_recorder")
                )

                // Tab 1: Recordings
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (allRecordings.isNotEmpty()) {
                                    Badge(containerColor = RecorderRed) {
                                        Text("${allRecordings.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null)
                        }
                    },
                    label = { Text("Videos") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = RecorderRed.copy(alpha = 0.2f),
                        selectedIconColor = RecorderRed,
                        selectedTextColor = RecorderRed
                    ),
                    modifier = Modifier.testTag("nav_item_recordings")
                )

                // Tab 2: System Monitor
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                    label = { Text("System") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = RecorderRed.copy(alpha = 0.2f),
                        selectedIconColor = RecorderRed,
                        selectedTextColor = RecorderRed
                    ),
                    modifier = Modifier.testTag("nav_item_monitor")
                )

                // Tab 3: Cloud Vault
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = {
                        if (isCloudStorageLocked) {
                            Icon(Icons.Default.Lock, contentDescription = "Cloud Locked", tint = AmberWarning)
                        } else {
                            Icon(Icons.Default.CloudDone, contentDescription = null)
                        }
                    },
                    label = { Text(if (isCloudStorageLocked) "Cloud (Lock)" else "Cloud") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = if (isCloudStorageLocked) AmberWarning.copy(alpha = 0.2f) else RecorderRed.copy(alpha = 0.2f),
                        selectedIconColor = if (isCloudStorageLocked) AmberWarning else RecorderRed,
                        selectedTextColor = if (isCloudStorageLocked) AmberWarning else RecorderRed
                    ),
                    modifier = Modifier.testTag("nav_item_cloud")
                )

                // Tab 4: Settings
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = RecorderRed.copy(alpha = 0.2f),
                        selectedIconColor = RecorderRed,
                        selectedTextColor = RecorderRed
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> RecorderScreen(
                    recordingState = recordingState,
                    countdownValue = countdownValue,
                    liveMetrics = liveMetrics,
                    config = config,
                    systemStats = systemStats,
                    onStartRecording = checkAndInitiateRecording,
                    onCancelCountdown = { viewModel.cancelCountdown() },
                    onPauseRecording = { viewModel.pauseRecording() },
                    onResumeRecording = { viewModel.resumeRecording() },
                    onStopRecording = {
                        viewModel.stopRecording()
                        activity?.let { AdManager.showInterstitial(it) }
                    },
                    onConfigChange = { newConfig -> viewModel.setConfig { newConfig } },
                    onNavigateToMonitor = { viewModel.setSelectedTab(2) },
                    isProUnlocked = isProUnlocked,
                    onRequestUnlockPro = requestUnlockPro,
                    onRequestOverlayPermission = { showOverlayPermissionDialog = true }
                )
                1 -> RecordingsScreen(
                    recordings = allRecordings,
                    onPlay = { rec -> viewModel.openPlayer(rec) },
                    onTrim = { rec -> viewModel.openTrimmer(rec) },
                    onBackup = { rec -> viewModel.backupRecordingToCloud(rec) },
                    onDelete = { rec -> viewModel.deleteRecording(rec) },
                    onDeleteMultiple = { recs -> viewModel.deleteRecordings(recs) },
                    onShare = { rec -> ShareUtils.shareRecording(context, rec) },
                    onDownload = handleDownloadWithRewardedAd,
                    onRename = { rec, newTitle -> viewModel.renameRecording(rec, newTitle) },
                    downloadingRecordingId = downloadingRecordingId,
                    cloudSyncInProgressId = cloudSyncInProgressId,
                    cloudSyncProgress = cloudSyncProgress,
                    isCloudStorageLocked = isCloudStorageLocked
                )
                2 -> SystemMonitorScreen(
                    systemStats = systemStats,
                    liveMetrics = liveMetrics,
                    recordingState = recordingState
                )
                3 -> CloudVaultScreen(
                    isPremiumUser = isPremiumUser,
                    recordings = allRecordings,
                    useEncryption = useEncryption,
                    onToggleEncryption = { viewModel.setEncryptionEnabled(it) },
                    onUpgradeClick = { viewModel.showPremiumUpgrade(true) },
                    onRestore = { rec -> viewModel.restoreRecordingFromCloud(rec) },
                    isCloudStorageLocked = isCloudStorageLocked
                )
                4 -> SettingsScreen(
                    config = config,
                    onConfigChange = { newConfig -> viewModel.setConfig { newConfig } },
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { viewModel.toggleTheme() },
                    isProUnlocked = isProUnlocked,
                    onRequestUnlockPro = requestUnlockPro,
                    onRequestOverlayPermission = { showOverlayPermissionDialog = true }
                )
            }
        }
    }

    // Modal Dialogs
    activePlayingRecording?.let { item ->
        VideoPlayerDialog(
            recording = item,
            onDismiss = { viewModel.closePlayer() },
            onOpenTrimmer = { rec -> viewModel.openTrimmer(rec) },
            onShare = { rec -> ShareUtils.shareRecording(context, rec) },
            onDownload = handleDownloadWithRewardedAd,
            onRename = { newTitle -> viewModel.renameRecording(item, newTitle) },
            isDownloading = downloadingRecordingId == item.id
        )
    }

    activeTrimmingRecording?.let { item ->
        VideoTrimmerDialog(
            recording = item,
            onDismiss = { viewModel.closeTrimmer() },
            onSaveTrimmed = { title, startMs, endMs ->
                viewModel.trimAndSaveClip(item, title, startMs, endMs)
                activity?.let { AdManager.showInterstitial(it) }
            }
        )
    }

    if (showPremiumDialog) {
        CloudUpgradeDialog(
            onDismiss = { viewModel.showPremiumUpgrade(false) },
            onPurchaseSuccess = { viewModel.purchasePremiumCloudVault() },
            isLocked = isCloudStorageLocked,
            onWatchAdToUnlock = {
                activity?.let {
                    AdManager.showRewarded(
                        activity = it,
                        onRewardEarned = {
                            viewModel.purchasePremiumCloudVault()
                            viewModel.showPremiumUpgrade(false)
                        }
                    )
                }
            }
        )
    }

    if (showProUnlockDialog) {
        ProFeatureUnlockDialog(
            featuresList = proUnlockFeaturesList,
            onDismiss = {
                showProUnlockDialog = false
                pendingStartRecordAfterUnlock = false
            },
            onWatchAdToUnlock = {
                showProUnlockDialog = false
                if (activity != null) {
                    var rewardedEarned = false
                    AdManager.showRewarded(
                        activity = activity,
                        onRewardEarned = {
                            rewardedEarned = true
                            viewModel.unlockProFeaturesForSession()
                            if (pendingStartRecordAfterUnlock) {
                                pendingStartRecordAfterUnlock = false
                                if (config.showFloatingControls && !FloatingControlOverlay.canDrawOverlay(context)) {
                                    showOverlayPermissionDialog = true
                                } else {
                                    onInitiateRecording()
                                }
                            }
                        },
                        onDismissed = {
                            if (!rewardedEarned && !isProUnlocked) {
                                viewModel.showFeedback("Watch the full video ad to unlock Pro features for this session!")
                            }
                        },
                        onAdUnavailable = {
                            viewModel.unlockProFeaturesForSession()
                            if (pendingStartRecordAfterUnlock) {
                                pendingStartRecordAfterUnlock = false
                                if (config.showFloatingControls && !FloatingControlOverlay.canDrawOverlay(context)) {
                                    showOverlayPermissionDialog = true
                                } else {
                                    onInitiateRecording()
                                }
                            }
                        }
                    )
                } else {
                    viewModel.unlockProFeaturesForSession()
                }
            },
            onRecordStandard = if (pendingStartRecordAfterUnlock) {
                {
                    showProUnlockDialog = false
                    pendingStartRecordAfterUnlock = false
                    viewModel.setConfig { ProFeatureManager.downgradeToStandard(it) }
                    if (config.showFloatingControls && !FloatingControlOverlay.canDrawOverlay(context)) {
                        showOverlayPermissionDialog = true
                    } else {
                        onInitiateRecording()
                    }
                }
            } else null
        )
    }

    if (showOverlayPermissionDialog) {
        OverlayPermissionDialog(
            onDismiss = { showOverlayPermissionDialog = false },
            onOpenSettings = {
                showOverlayPermissionDialog = false
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    context.startActivity(intent)
                }
            },
            onContinueWithout = {
                showOverlayPermissionDialog = false
                onInitiateRecording()
            }
        )
    }
}
