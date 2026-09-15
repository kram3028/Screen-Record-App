package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

interface ScreenRecorderActionListener {
    fun onPauseRequested()
    fun onResumeRequested()
    fun onStopRequested()
}

class ScreenRecorderService : Service() {

    companion object {
        const val CHANNEL_ID_RECORDING = "screen_recording_channel"
        const val CHANNEL_ID_STOPPED = "screen_recording_stopped_channel"
        const val NOTIFICATION_ID_RECORDING = 101
        const val NOTIFICATION_ID_STOPPED = 102

        // Backwards compatibility aliases
        const val CHANNEL_ID = CHANNEL_ID_RECORDING
        const val NOTIFICATION_ID = NOTIFICATION_ID_RECORDING

        const val ACTION_START = "com.example.screenrecorder.ACTION_START"
        const val ACTION_PAUSE = "com.example.screenrecorder.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.screenrecorder.ACTION_RESUME"
        const val ACTION_STOP = "com.example.screenrecorder.ACTION_STOP"
        const val ACTION_STOP_SERVICE_ONLY = "com.example.screenrecorder.ACTION_STOP_SERVICE_ONLY"
        const val ACTION_DISMISS_STOPPED_NOTIFICATION = "com.example.screenrecorder.ACTION_DISMISS_STOPPED_NOTIFICATION"

        const val EXTRA_SHOW_OVERLAY = "extra_show_overlay"
        const val EXTRA_SESSION_ID = "extra_session_id"

        var actionListener: ScreenRecorderActionListener? = null

        fun stopServiceDirectly(context: Context) {
            try {
                val intent = Intent(context, ScreenRecorderService::class.java).apply {
                    action = ACTION_STOP_SERVICE_ONLY
                }
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java) ?: return

                val recordingChannel = NotificationChannel(
                    CHANNEL_ID_RECORDING,
                    "Active Screen Recording",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows live status and stop controls while screen recording is active"
                    setShowBadge(false)
                }

                val stoppedChannel = NotificationChannel(
                    CHANNEL_ID_STOPPED,
                    "Screen Recording Stopped",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Persistent status when screen recording has stopped"
                    setShowBadge(true)
                }

                manager.createNotificationChannel(recordingChannel)
                manager.createNotificationChannel(stoppedChannel)
            }
        }

        fun showStoppedNotification(context: Context, title: String, contentText: String) {
            try {
                createNotificationChannels(context)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_SELECTED_TAB, 1)
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context, 10, appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val dismissIntent = Intent(context, ScreenRecorderService::class.java).apply {
                    action = ACTION_DISMISS_STOPPED_NOTIFICATION
                }
                val dismissPendingIntent = PendingIntent.getService(
                    context, 11, dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID_STOPPED)
                    .setContentTitle(title)
                    .setContentText(contentText)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentIntent(contentPendingIntent)
                    .setOngoing(true) // Persistent notification while recording is stopped
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .addAction(
                        android.R.drawable.ic_menu_view,
                        "View Video",
                        contentPendingIntent
                    )
                    .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Dismiss",
                        dismissPendingIntent
                    )
                    .build()

                manager.notify(NOTIFICATION_ID_STOPPED, notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun dismissStoppedNotification(context: Context) {
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(NOTIFICATION_ID_STOPPED)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val binder = LocalBinder()
    private val isRecordingActive = java.util.concurrent.atomic.AtomicBoolean(false)
    private var floatingOverlay: FloatingControlOverlay? = null

    inner class LocalBinder : Binder() {
        fun getService(): ScreenRecorderService = this@ScreenRecorderService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                isRecordingActive.set(true)
                // Dismiss any lingering stopped notification from previous runs
                dismissStoppedNotification(this)

                val notification = buildNotification("Screen recording in progress...", isPaused = false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val hasMicPermission = ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMicPermission) {
                            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        }
                    }
                    ServiceCompat.startForeground(this, NOTIFICATION_ID_RECORDING, notification, type)
                } else {
                    startForeground(NOTIFICATION_ID_RECORDING, notification)
                }

                val showOverlay = intent.getBooleanExtra(EXTRA_SHOW_OVERLAY, true)
                if (showOverlay && FloatingControlOverlay.canDrawOverlay(this)) {
                    if (floatingOverlay == null) {
                        floatingOverlay = FloatingControlOverlay(this, object : FloatingControlOverlay.OverlayActionListener {
                            override fun onPauseClicked() {
                                actionListener?.onPauseRequested()
                                updateNotification("Recording paused", isPaused = true)
                            }

                            override fun onResumeClicked() {
                                actionListener?.onResumeRequested()
                                updateNotification("Screen recording in progress...", isPaused = false)
                            }

                            override fun onStopClicked() {
                                if (isRecordingActive.compareAndSet(true, false)) {
                                    actionListener?.onStopRequested()
                                }
                                showStoppedNotification(
                                    this@ScreenRecorderService,
                                    "Screen Recording Stopped",
                                    "Processing recording..."
                                )
                                stopRecordingService()
                            }
                        })
                    }
                    floatingOverlay?.show()
                }
            }
            ACTION_PAUSE -> {
                updateNotification("Recording paused", isPaused = true)
                floatingOverlay?.setPausedState(true)
                actionListener?.onPauseRequested()
            }
            ACTION_RESUME -> {
                updateNotification("Screen recording in progress...", isPaused = false)
                floatingOverlay?.setPausedState(false)
                actionListener?.onResumeRequested()
            }
            ACTION_STOP -> {
                if (isRecordingActive.compareAndSet(true, false)) {
                    actionListener?.onStopRequested()
                }
                showStoppedNotification(
                    this,
                    "Screen Recording Stopped",
                    "Screen recording ended • Tap to view"
                )
                stopRecordingService()
            }
            ACTION_STOP_SERVICE_ONLY -> {
                stopRecordingService()
            }
            ACTION_DISMISS_STOPPED_NOTIFICATION -> {
                dismissStoppedNotification(this)
            }
        }
        return START_NOT_STICKY
    }

    private fun stopRecordingService() {
        isRecordingActive.set(false)
        floatingOverlay?.dismiss()
        floatingOverlay = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(contentText: String, isPaused: Boolean): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, ScreenRecorderService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this, 1, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenRecorderService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_RECORDING)
            .setContentTitle("Screen Recording Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true) // Persistent notification during active recording
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pauseResumePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Recording", // Action button to stop recording directly from notification shade
                stopPendingIntent
            )

        return builder.build()
    }

    fun updateNotification(contentText: String, isPaused: Boolean) {
        val notification = buildNotification(contentText, isPaused)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_RECORDING, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingOverlay?.dismiss()
        floatingOverlay = null
        isRecordingActive.set(false)
    }
}
