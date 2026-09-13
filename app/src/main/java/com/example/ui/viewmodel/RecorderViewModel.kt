package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.config.CloudConfig
import com.example.data.db.AppDatabase
import com.example.data.model.AudioSourceMode
import com.example.data.model.LiveRecordingMetrics
import com.example.data.model.RecorderConfig
import com.example.data.model.RecordingEntity
import com.example.data.model.RecordingState
import com.example.data.model.VideoFormat
import com.example.data.model.VideoResolution
import com.example.data.repository.RecordingRepository
import com.example.data.sync.FirebaseSyncManager
import com.example.data.sync.FirebaseSyncState
import com.example.monitor.SystemMonitor
import com.example.monitor.SystemResourceStats
import com.example.recorder.ScreenRecorderEngine
import com.example.service.ScreenRecorderActionListener
import com.example.service.ScreenRecorderService
import com.example.util.VideoDownloadUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecorderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = RecordingRepository(application, db.recordingDao())
    private val systemMonitor = SystemMonitor(application)
    private val engine = ScreenRecorderEngine(application, viewModelScope)
    private val firebaseSyncManager = FirebaseSyncManager.getInstance(application)

    val firebaseSyncState: StateFlow<FirebaseSyncState> = firebaseSyncManager.syncState

    val allRecordings: StateFlow<List<RecordingEntity>> = repository.allRecordings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val recordingState: StateFlow<RecordingState> = engine.recordingState
    val liveMetrics: StateFlow<LiveRecordingMetrics> = engine.liveMetrics

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _countdownValue = MutableStateFlow(0)
    val countdownValue: StateFlow<Int> = _countdownValue.asStateFlow()

    private val _config = MutableStateFlow(RecorderConfig())
    val config: StateFlow<RecorderConfig> = _config.asStateFlow()

    val systemStats: StateFlow<SystemResourceStats> = systemMonitor.monitorStatsFlow(1000L)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = systemMonitor.sampleStats()
        )

    // Cloud Vault State
    private val _isCloudStorageLocked = MutableStateFlow(CloudConfig.IS_CLOUD_STORAGE_LOCKED)
    val isCloudStorageLocked: StateFlow<Boolean> = _isCloudStorageLocked.asStateFlow()

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    private val _useClientSideEncryption = MutableStateFlow(true)
    val useClientSideEncryption: StateFlow<Boolean> = _useClientSideEncryption.asStateFlow()

    private val _cloudSyncInProgressId = MutableStateFlow<Long?>(null)
    val cloudSyncInProgressId: StateFlow<Long?> = _cloudSyncInProgressId.asStateFlow()

    private val _cloudSyncProgress = MutableStateFlow(0f)
    val cloudSyncProgress: StateFlow<Float> = _cloudSyncProgress.asStateFlow()

    // Dialogs & Modals
    private val _downloadingRecordingId = MutableStateFlow<Long?>(null)
    val downloadingRecordingId: StateFlow<Long?> = _downloadingRecordingId.asStateFlow()

    private val _activeTrimmingRecording = MutableStateFlow<RecordingEntity?>(null)
    val activeTrimmingRecording: StateFlow<RecordingEntity?> = _activeTrimmingRecording.asStateFlow()

    private val _activePlayingRecording = MutableStateFlow<RecordingEntity?>(null)
    val activePlayingRecording: StateFlow<RecordingEntity?> = _activePlayingRecording.asStateFlow()

    private val _showPremiumDialog = MutableStateFlow(false)
    val showPremiumDialog: StateFlow<Boolean> = _showPremiumDialog.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Rewarded Ad Gating for Pro Features (1080p, 2K, 120 FPS, 24 Mbps)
    private val _isProFeaturesUnlockedForSession = MutableStateFlow(false)
    val isProFeaturesUnlockedForSession: StateFlow<Boolean> = _isProFeaturesUnlockedForSession.asStateFlow()

    private var countdownJob: Job? = null
    private var pendingResultCode = 0
    private var pendingData: Intent? = null
    private val isStoppingAtomic = java.util.concurrent.atomic.AtomicBoolean(false)

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            cleanDuplicateRecordings()
        }

        // Connect floating overlay and notification actions with recorder engine
        ScreenRecorderService.actionListener = object : ScreenRecorderActionListener {
            override fun onPauseRequested() {
                engine.pauseRecording()
            }

            override fun onResumeRequested() {
                engine.resumeRecording()
            }

            override fun onStopRequested() {
                stopRecording()
            }
        }
    }

    fun unlockProFeaturesForSession() {
        _isProFeaturesUnlockedForSession.value = true
        _userFeedbackMessage.value = "Pro recording features unlocked for this session!"
    }

    fun resetProFeaturesLock() {
        _isProFeaturesUnlockedForSession.value = false
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setDarkTheme(dark: Boolean) {
        _isDarkTheme.value = dark
    }

    fun setConfig(updater: (RecorderConfig) -> RecorderConfig) {
        _config.value = updater(_config.value)
    }

    fun updateResolution(resolution: VideoResolution) {
        _config.value = _config.value.copy(resolution = resolution)
    }

    fun updateFormat(format: VideoFormat) {
        _config.value = _config.value.copy(format = format)
    }

    fun updateFps(fps: Int) {
        _config.value = _config.value.copy(fps = fps)
    }

    fun updateBitrate(bitrateMbps: Int) {
        _config.value = _config.value.copy(bitrateMbps = bitrateMbps)
    }

    fun updateAudioSource(source: AudioSourceMode) {
        _config.value = _config.value.copy(audioSource = source)
    }

    fun toggleFloatingControls(enable: Boolean) {
        _config.value = _config.value.copy(showFloatingControls = enable)
    }

    fun setEncryptionEnabled(enabled: Boolean) {
        _useClientSideEncryption.value = enabled
    }

    fun openTrimmer(recording: RecordingEntity) {
        _activeTrimmingRecording.value = recording
    }

    fun closeTrimmer() {
        _activeTrimmingRecording.value = null
    }

    fun openPlayer(recording: RecordingEntity) {
        _activePlayingRecording.value = recording
    }

    fun closePlayer() {
        _activePlayingRecording.value = null
    }

    fun showPremiumUpgrade(show: Boolean) {
        _showPremiumDialog.value = show
    }

    fun clearFeedback() {
        _userFeedbackMessage.value = null
    }

    /**
     * Handles recording request initiation with countdown timer
     */
    fun startRecordingFlow(resultCode: Int = -1, data: Intent? = null) {
        pendingResultCode = resultCode
        pendingData = data

        // Pre-launch foreground service so it's fully active when mediaProjection is requested
        val app = getApplication<Application>()
        val serviceIntent = Intent(app, ScreenRecorderService::class.java).apply {
            action = ScreenRecorderService.ACTION_START
            putExtra(ScreenRecorderService.EXTRA_SHOW_OVERLAY, _config.value.showFloatingControls)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(serviceIntent)
            } else {
                app.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val countdownSecs = _config.value.countdownSeconds
        if (countdownSecs <= 0) {
            viewModelScope.launch {
                delay(150)
                startServiceAndEngine()
            }
        } else {
            countdownJob?.cancel()
            _countdownValue.value = countdownSecs
            countdownJob = viewModelScope.launch {
                for (sec in countdownSecs downTo 1) {
                    _countdownValue.value = sec
                    delay(1000)
                }
                _countdownValue.value = 0
                startServiceAndEngine()
            }
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _countdownValue.value = 0
        val app = getApplication<Application>()
        val serviceIntent = Intent(app, ScreenRecorderService::class.java).apply {
            action = ScreenRecorderService.ACTION_STOP
        }
        app.startService(serviceIntent)
    }

    private fun startServiceAndEngine() {
        val uniqueSessionId = "session_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().replace("-", "").take(8)}"
        _activeSessionId.value = uniqueSessionId

        val app = getApplication<Application>()
        val serviceIntent = Intent(app, ScreenRecorderService::class.java).apply {
            action = ScreenRecorderService.ACTION_START
            putExtra(ScreenRecorderService.EXTRA_SHOW_OVERLAY, _config.value.showFloatingControls)
            putExtra(ScreenRecorderService.EXTRA_SESSION_ID, uniqueSessionId)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(serviceIntent)
            } else {
                app.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        engine.startRecording(pendingResultCode, pendingData, _config.value, sessionId = uniqueSessionId)
        _userFeedbackMessage.value = "Recording started (${_config.value.resolution.label}, ${_config.value.format.name})"
    }

    fun pauseRecording() {
        engine.pauseRecording()
        val app = getApplication<Application>()
        val serviceIntent = Intent(app, ScreenRecorderService::class.java).apply {
            action = ScreenRecorderService.ACTION_PAUSE
        }
        app.startService(serviceIntent)
    }

    fun resumeRecording() {
        engine.resumeRecording()
        val app = getApplication<Application>()
        val serviceIntent = Intent(app, ScreenRecorderService::class.java).apply {
            action = ScreenRecorderService.ACTION_RESUME
        }
        app.startService(serviceIntent)
    }

    fun stopRecording() {
        if (!isStoppingAtomic.compareAndSet(false, true)) {
            Log.w("RecorderViewModel", "stopRecording already in progress, ignoring duplicate trigger")
            return
        }

        viewModelScope.launch {
            try {
                if (!engine.isRecording() && !engine.isPaused()) {
                    Log.w("RecorderViewModel", "Engine is not recording or paused, aborting stop")
                    return@launch
                }

                val result = withContext(Dispatchers.Default) {
                    engine.stopRecording()
                }

                val app = getApplication<Application>()
                // Stop foreground service cleanly without circular invocation
                ScreenRecorderService.stopServiceDirectly(app)

                // Lock Pro features again after recording session completes!
                _isProFeaturesUnlockedForSession.value = false

                val file = result.first
                val duration = result.second
                val sessionToSave = engine.getLastCompletedSessionId() ?: _activeSessionId.value ?: "session_${System.currentTimeMillis()}"

                if (file != null && file.exists()) {
                    val timeStamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
                    val title = "Screen Record - $timeStamp"
                    val savedId = repository.saveRecording(
                        title = title,
                        file = file,
                        durationMs = duration,
                        resolution = _config.value.resolution.label,
                        format = _config.value.format,
                        fps = _config.value.fps,
                        bitrateMbps = _config.value.bitrateMbps,
                        audioSource = _config.value.audioSource.label,
                        sessionId = sessionToSave
                    )
                    if (savedId > 0L) {
                        val mb = file.length().toDouble() / (1024.0 * 1024.0)
                        val sizeFormatted = String.format(Locale.getDefault(), "%.1f MB", mb.coerceAtLeast(0.1))
                        _userFeedbackMessage.value = "Saved $title ($sizeFormatted)"
                        _selectedTab.value = 1 // Switch to Recordings tab
                        // Auto-sync metadata to Firebase if connected
                        firebaseSyncManager.scheduleSync("New recording saved")
                    }
                }
            } catch (e: Exception) {
                Log.e("RecorderViewModel", "Error in stopRecording", e)
            } finally {
                _activeSessionId.value = null
                delay(300)
                isStoppingAtomic.set(false)
            }
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            repository.deleteRecording(recording)
            _userFeedbackMessage.value = "Deleted ${recording.title}"
        }
    }

    fun deleteRecordings(recordings: List<RecordingEntity>) {
        if (recordings.isEmpty()) return
        viewModelScope.launch {
            val totalBytes = recordings.sumOf { it.fileSizeBytes }
            val mb = totalBytes.toDouble() / (1024.0 * 1024.0)
            val sizeSuffix = if (mb >= 0.1) String.format(Locale.getDefault(), " (%.1f MB freed)", mb) else ""
            repository.deleteRecordings(recordings)
            _userFeedbackMessage.value = "Deleted ${recordings.size} recordings$sizeSuffix"
        }
    }

    /**
     * Deduplicates existing recordings that share identical file paths directly in database
     */
    fun cleanDuplicateRecordings() {
        viewModelScope.launch {
            val removed = repository.cleanDuplicateRecordings()
            if (removed > 0) {
                _userFeedbackMessage.value = "Cleaned up $removed duplicate entries"
            }
        }
    }

    fun renameRecording(recording: RecordingEntity, newTitle: String) {
        val trimmedTitle = newTitle.trim()
        if (trimmedTitle.isNotEmpty()) {
            viewModelScope.launch {
                repository.renameRecording(recording.id, trimmedTitle)
                _userFeedbackMessage.value = "Renamed to \"$trimmedTitle\""
            }
        }
    }

    fun trimAndSaveClip(original: RecordingEntity, newTitle: String, startMs: Long, endMs: Long) {
        viewModelScope.launch {
            val trimmed = repository.trimAndSaveClip(original, newTitle, startMs, endMs)
            if (trimmed != null) {
                _userFeedbackMessage.value = "Clip trimmed & saved: $newTitle"
                closeTrimmer()
            } else {
                _userFeedbackMessage.value = "Failed to trim video"
            }
        }
    }

    fun backupRecordingToCloud(recording: RecordingEntity) {
        if (_isCloudStorageLocked.value) {
            _userFeedbackMessage.value = CloudConfig.LOCK_SNACKBAR_MESSAGE
            return
        }

        if (!_isPremiumUser.value) {
            _showPremiumDialog.value = true
            return
        }

        viewModelScope.launch {
            _cloudSyncInProgressId.value = recording.id
            _cloudSyncProgress.value = 0.1f
            val success = repository.backupToCloud(
                recording = recording,
                useEncryption = _useClientSideEncryption.value,
                onProgress = { p -> _cloudSyncProgress.value = p }
            )
            _cloudSyncInProgressId.value = null
            _cloudSyncProgress.value = 0f
            if (success) {
                _userFeedbackMessage.value = "Backed up '${recording.title}' to Cloud Vault (AES-256 Encrypted)"
            } else {
                _userFeedbackMessage.value = "Cloud backup failed. Check connection."
            }
        }
    }

    fun restoreRecordingFromCloud(recording: RecordingEntity) {
        if (_isCloudStorageLocked.value) {
            _userFeedbackMessage.value = CloudConfig.LOCK_SNACKBAR_MESSAGE
            return
        }

        viewModelScope.launch {
            val success = repository.restoreFromCloud(recording)
            if (success) {
                _userFeedbackMessage.value = "Restored '${recording.title}' from Cloud"
            } else {
                _userFeedbackMessage.value = "Failed to restore clip from Cloud"
            }
        }
    }

    fun purchasePremiumCloudVault() {
        if (_isCloudStorageLocked.value) {
            _userFeedbackMessage.value = CloudConfig.LOCK_SNACKBAR_MESSAGE
            return
        }

        _isPremiumUser.value = true
        _showPremiumDialog.value = false
        _userFeedbackMessage.value = "Unlocked Cloud Vault Pro (100 GB + AES-256 Zero-Knowledge Encryption)!"
    }

    fun setCloudStorageLocked(locked: Boolean) {
        _isCloudStorageLocked.value = locked
    }

    val userData = repository.userData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun downloadRecordingToPhone(recording: RecordingEntity, onComplete: ((Boolean) -> Unit)? = null) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _downloadingRecordingId.value = recording.id
            try {
                when (val result = VideoDownloadUtils.saveVideoToDevice(app, recording)) {
                    is VideoDownloadUtils.DownloadResult.Success -> {
                        // Securely persist download history and user metrics in free local SQLite (Room)
                        repository.recordDownload(
                            recording = recording,
                            destinationPath = result.destination,
                            contentUri = result.uri?.toString() ?: "",
                            rewardAdVerified = true
                        )
                        _userFeedbackMessage.value = "Saved to Gallery: ${result.destination}"
                        onComplete?.invoke(true)
                    }
                    is VideoDownloadUtils.DownloadResult.Error -> {
                        _userFeedbackMessage.value = "Download failed: ${result.message}"
                        onComplete?.invoke(false)
                    }
                }
            } catch (e: Exception) {
                _userFeedbackMessage.value = "Download failed: ${e.localizedMessage}"
                onComplete?.invoke(false)
            } finally {
                _downloadingRecordingId.value = null
            }
        }
    }

    fun showFeedback(message: String) {
        _userFeedbackMessage.value = message
    }

    fun triggerFirebaseSync() {
        firebaseSyncManager.scheduleSync("Manual user request")
    }

    fun setFirebaseAutoSync(enabled: Boolean) {
        firebaseSyncManager.setAutoSyncEnabled(enabled)
    }

    fun setFirebaseProjectId(projectId: String) {
        firebaseSyncManager.setProjectId(projectId)
    }

    override fun onCleared() {
        super.onCleared()
        ScreenRecorderService.actionListener = null
    }
}
