package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.RecordingEntity
import com.example.util.NetworkMonitor
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Data class representing current synchronization status.
 */
data class FirebaseSyncState(
    val isSyncing: Boolean = false,
    val isAutoSyncEnabled: Boolean = true,
    val isInternetConnected: Boolean = false,
    val unsyncedCount: Int = 0,
    val syncedCount: Int = 0,
    val totalCount: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val statusMessage: String = "Ready",
    val projectId: String = "",
    val isConfigured: Boolean = false
)

/**
 * Manages automatic and manual synchronization of recording metadata to Firebase Firestore.
 * Listens for internet connectivity changes and automatically uploads unsynced recordings.
 */
class FirebaseSyncManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("firebase_sync_prefs", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val networkMonitor = NetworkMonitor(context)
    private val db = AppDatabase.getInstance(context)
    private val recordingDao = db.recordingDao()
    private val userDataDao = db.userDataDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _syncState = MutableStateFlow(
        FirebaseSyncState(
            isAutoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true),
            projectId = prefs.getString(KEY_PROJECT_ID, "") ?: "",
            lastSyncTimestamp = prefs.getLong(KEY_LAST_SYNC, 0L),
            isInternetConnected = networkMonitor.isCurrentlyConnected(),
            isConfigured = checkIsConfigured()
        )
    )
    val syncState: StateFlow<FirebaseSyncState> = _syncState.asStateFlow()

    companion object {
        private const val TAG = "FirebaseSyncManager"
        private const val KEY_AUTO_SYNC = "key_auto_sync_enabled"
        private const val KEY_PROJECT_ID = "key_firebase_project_id"
        private const val KEY_LAST_SYNC = "key_last_sync_timestamp"

        const val DEFAULT_PROJECT_ID = "screen-recorder-metadata"
        const val DEFAULT_APP_ID = "1:500606938510:android:fa78054cc1b1f9cf5a48df"
        const val DEFAULT_API_KEY = "AIzaSyDYH2nLJRK0ix4NxsEppBlNk6HlsYlnalM"
        const val DEFAULT_STORAGE_BUCKET = "screen-recorder-metadata.firebasestorage.app"

        const val COLLECTION_RECORDINGS = "recording_metadata"
        const val COLLECTION_PROFILES = "user_profiles"

        @Volatile
        private var instance: FirebaseSyncManager? = null

        fun getInstance(context: Context): FirebaseSyncManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseSyncManager(context.applicationContext).also {
                    instance = it
                    it.initialize()
                }
            }
        }
    }

    private fun initialize() {
        // Start network monitoring
        networkMonitor.startMonitoring()

        // Trigger sync on startup if internet is already available
        if (networkMonitor.isCurrentlyConnected()) {
            Log.d(TAG, "Internet available on launch. Scheduling metadata sync...")
            scheduleSync(triggerReason = "Initial startup")
        }

        // Listen for network connectivity restored
        networkMonitor.setOnConnectedListener {
            Log.d(TAG, "Network connection detected! Checking for unsynced data...")
            _syncState.value = _syncState.value.copy(
                isInternetConnected = true
            )
            scheduleSync(triggerReason = "Internet connection restored")
        }

        // Monitor counts from Room database
        scope.launch {
            recordingDao.getAllRecordings().collect { allList ->
                val unsynced = allList.count { !it.isFirebaseSynced }
                val synced = allList.count { it.isFirebaseSynced }
                _syncState.value = _syncState.value.copy(
                    totalCount = allList.size,
                    unsyncedCount = unsynced,
                    syncedCount = synced,
                    isInternetConnected = networkMonitor.isCurrentlyConnected()
                )
            }
        }
    }

    private fun checkIsConfigured(): Boolean {
        return true
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
        _syncState.value = _syncState.value.copy(isAutoSyncEnabled = enabled)
        if (enabled && networkMonitor.isCurrentlyConnected()) {
            scheduleSync("Auto-sync enabled")
        }
    }

    fun setProjectId(projectId: String) {
        val trimmed = projectId.trim()
        prefs.edit().putString(KEY_PROJECT_ID, trimmed).apply()
        _syncState.value = _syncState.value.copy(
            projectId = trimmed,
            isConfigured = checkIsConfigured()
        )
        if (trimmed.isNotEmpty() && networkMonitor.isCurrentlyConnected()) {
            scheduleSync("Project ID updated")
        }
    }

    /**
     * Triggers synchronization asynchronously in the background.
     */
    fun scheduleSync(triggerReason: String = "Manual") {
        scope.launch {
            performSync(triggerReason)
        }
    }

    /**
     * Performs synchronization of unsynced metadata.
     */
    suspend fun performSync(triggerReason: String = "Manual"): Boolean = withContext(Dispatchers.IO) {
        if (!syncMutex.tryLock()) {
            Log.d(TAG, "Sync already in progress, skipping concurrent request")
            return@withContext false
        }

        try {
            val isConnected = networkMonitor.isCurrentlyConnected()
            _syncState.value = _syncState.value.copy(isInternetConnected = isConnected)

            if (!isConnected) {
                _syncState.value = _syncState.value.copy(
                    statusMessage = "Offline: Sync will resume automatically when internet connects"
                )
                Log.d(TAG, "Device is offline. Unsynced data will sync upon network reconnection.")
                return@withContext false
            }

            val unsyncedList = recordingDao.getUnsyncedRecordings()
            if (unsyncedList.isEmpty()) {
                val lastSyncTime = System.currentTimeMillis()
                prefs.edit().putLong(KEY_LAST_SYNC, lastSyncTime).apply()
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    lastSyncTimestamp = lastSyncTime,
                    statusMessage = "All recording metadata is up-to-date with Firebase"
                )
                Log.d(TAG, "No unsynced recordings found. Up-to-date.")
                return@withContext true
            }

            _syncState.value = _syncState.value.copy(
                isSyncing = true,
                statusMessage = "Syncing ${unsyncedList.size} recording(s) to Firebase ($triggerReason)..."
            )

            val success = tryUploadMetadata(unsyncedList)

            if (success) {
                val now = System.currentTimeMillis()
                recordingDao.markMultipleAsSynced(unsyncedList.map { it.id }, now)
                prefs.edit().putLong(KEY_LAST_SYNC, now).apply()

                val updatedUnsynced = recordingDao.getUnsyncedRecordings().size
                val updatedSynced = recordingDao.getAllRecordingsOnce().count { it.isFirebaseSynced }

                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    unsyncedCount = updatedUnsynced,
                    syncedCount = updatedSynced,
                    lastSyncTimestamp = now,
                    statusMessage = "Successfully synchronized ${unsyncedList.size} recording(s) to Firebase"
                )
                Log.d(TAG, "Successfully synced ${unsyncedList.size} recordings to Firebase!")
                true
            } else {
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    statusMessage = "Sync paused. Check Firebase credentials or internet connection."
                )
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Firebase sync", e)
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                statusMessage = "Sync failed: ${e.localizedMessage ?: "Unknown error"}"
            )
            false
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun tryUploadMetadata(recordings: List<RecordingEntity>): Boolean {
        // Strategy 1: Use FirebaseFirestore SDK if initialized
        var initialized = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }

        val targetProjectId = prefs.getString(KEY_PROJECT_ID, "").takeIf { !it.isNullOrBlank() } ?: DEFAULT_PROJECT_ID

        // If not initialized via google-services.json, initialize FirebaseApp programmatically
        if (!initialized) {
            try {
                val options = FirebaseOptions.Builder()
                    .setProjectId(targetProjectId)
                    .setApplicationId(DEFAULT_APP_ID)
                    .setApiKey(DEFAULT_API_KEY)
                    .setStorageBucket(DEFAULT_STORAGE_BUCKET)
                    .build()
                FirebaseApp.initializeApp(context, options)
                initialized = true
                Log.d(TAG, "Initialized FirebaseApp with project: $targetProjectId")
            } catch (e: Exception) {
                Log.w(TAG, "Could not initialize FirebaseApp programmatically: ${e.message}")
            }
        }

        if (initialized) {
            val sdkSuccess = uploadViaFirestoreSdk(recordings)
            if (sdkSuccess) return true
        }

        // Strategy 2: Firestore REST API with API key fallback
        return uploadViaFirestoreRest(targetProjectId, recordings)
    }

    private suspend fun uploadViaFirestoreSdk(recordings: List<RecordingEntity>): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val batch = firestore.batch()

            val now = System.currentTimeMillis()
            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            val osVersion = Build.VERSION.RELEASE

            for (recording in recordings) {
                val docRef = firestore.collection(COLLECTION_RECORDINGS)
                    .document("recording_${recording.id}")

                val data = hashMapOf(
                    "id" to recording.id,
                    "title" to recording.title,
                    "durationMs" to recording.durationMs,
                    "fileSizeBytes" to recording.fileSizeBytes,
                    "resolution" to recording.resolution,
                    "format" to recording.format,
                    "fps" to recording.fps,
                    "bitrateMbps" to recording.bitrateMbps,
                    "audioSource" to recording.audioSource,
                    "createdAt" to recording.createdAt,
                    "sessionId" to recording.sessionId,
                    "syncedAt" to now,
                    "deviceModel" to deviceModel,
                    "osVersion" to osVersion
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            // Sync user activity metrics
            val userDocRef = firestore.collection(COLLECTION_PROFILES)
                .document("user_metrics")
            val userProfile = userDataDao.getUserData()
            val userProfileData = hashMapOf(
                "userId" to (userProfile?.userId ?: "local_default_user"),
                "totalSavedVideosCount" to recordings.size,
                "lastActiveAt" to (userProfile?.lastActiveAt ?: now),
                "lastSyncTimestamp" to now,
                "deviceModel" to deviceModel
            )
            batch.set(userDocRef, userProfileData, SetOptions.merge())

            batch.commit().awaitResult()
            Log.d(TAG, "Batch commit to Firestore succeeded for ${recordings.size} records")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firestore SDK upload error", e)
            false
        }
    }

    private suspend fun uploadViaFirestoreRest(projectId: String, recordings: List<RecordingEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

                for (recording in recordings) {
                    val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$COLLECTION_RECORDINGS?documentId=recording_${recording.id}&key=$DEFAULT_API_KEY"

                    val fields = JSONObject().apply {
                        put("id", JSONObject().put("integerValue", recording.id))
                        put("title", JSONObject().put("stringValue", recording.title))
                        put("durationMs", JSONObject().put("integerValue", recording.durationMs))
                        put("fileSizeBytes", JSONObject().put("integerValue", recording.fileSizeBytes))
                        put("resolution", JSONObject().put("stringValue", recording.resolution))
                        put("format", JSONObject().put("stringValue", recording.format))
                        put("fps", JSONObject().put("integerValue", recording.fps))
                        put("bitrateMbps", JSONObject().put("integerValue", recording.bitrateMbps))
                        put("audioSource", JSONObject().put("stringValue", recording.audioSource))
                        put("createdAt", JSONObject().put("integerValue", recording.createdAt))
                        put("sessionId", JSONObject().put("stringValue", recording.sessionId))
                        put("syncedAt", JSONObject().put("integerValue", now))
                        put("deviceModel", JSONObject().put("stringValue", deviceModel))
                    }

                    val jsonPayload = JSONObject().apply {
                        put("fields", fields)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val requestBody = jsonPayload.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (!response.isSuccessful && response.code != 409) { // 409 = already exists
                        Log.w(TAG, "REST sync response: ${response.code} - ${response.message}")
                    }
                    response.close()
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Firestore REST upload error", e)
                false
            }
        }
    }
}

/**
 * Extension function to await Google Play Services Task in Kotlin coroutines safely.
 */
private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        continuation.resume(result)
    }
    addOnFailureListener { exception ->
        continuation.resumeWithException(exception)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}
