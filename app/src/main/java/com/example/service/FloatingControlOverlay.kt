package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.R
import kotlin.math.abs

/**
 * Real System Floating Overlay Window for Screen Recording.
 * Floats over all apps and the home screen using WindowManager and TYPE_APPLICATION_OVERLAY.
 */
class FloatingControlOverlay(
    private val context: Context,
    private val listener: OverlayActionListener
) {

    interface OverlayActionListener {
        fun onPauseClicked()
        fun onResumeClicked()
        fun onStopClicked()
    }

    companion object {
        private const val TAG = "FloatingControlOverlay"
        fun canDrawOverlay(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayRootView: View? = null
    private var isAttached = false
    private var isExpanded = false
    private var isPaused = false

    private lateinit var layoutParams: WindowManager.LayoutParams
    private var bubbleContainer: FrameLayout? = null
    private var bubbleIcon: ImageView? = null
    private var controlsBar: LinearLayout? = null
    private var pauseResumeBtn: ImageView? = null
    private var timerText: TextView? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0L
    private var isTimerRunning = false

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning && !isPaused) {
                elapsedSeconds++
                updateTimerDisplay()
            }
            mainHandler.postDelayed(this, 1000)
        }
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isAttached) return
        if (!canDrawOverlay(context)) {
            Log.w(TAG, "Cannot draw overlay: SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = dpToPx(16f)
                y = dpToPx(160f)
            }

            val rootLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#E612151D"))
                    cornerRadius = dpToPx(26f).toFloat()
                    setStroke(dpToPx(1.5f), Color.parseColor("#40FF3B5C"))
                }
                background = bg
                setPadding(dpToPx(4f), dpToPx(4f), dpToPx(4f), dpToPx(4f))
            }

            // 1. Draggable Floating Bubble
            val bubble = FrameLayout(context).apply {
                val size = dpToPx(48f)
                layoutParams = LinearLayout.LayoutParams(size, size)
                val bubbleBg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#E50914"))
                }
                background = bubbleBg
            }

            val bubbleInnerIcon = ImageView(context).apply {
                val iconSize = dpToPx(24f)
                val lp = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
                layoutParams = lp
                setImageResource(R.drawable.ic_overlay_pause)
                setColorFilter(Color.WHITE)
            }
            bubble.addView(bubbleInnerIcon)
            bubbleContainer = bubble
            bubbleIcon = bubbleInnerIcon

            // Touch listener for dragging & tapping bubble
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isDragging = false
            val touchSlop = dpToPx(8f)

            bubble.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                            isDragging = true
                            layoutParams.x = (initialX + dx).toInt()
                            layoutParams.y = (initialY + dy).toInt()
                            try {
                                windowManager.updateViewLayout(overlayRootView, layoutParams)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating overlay position", e)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            toggleExpanded()
                        }
                        true
                    }
                    else -> false
                }
            }

            rootLayout.addView(bubble)

            // 2. Expandable Controls Bar
            val controls = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                visibility = View.GONE
                setPadding(dpToPx(8f), 0, dpToPx(6f), 0)
            }

            // Live Timer Text
            val timerTv = TextView(context).apply {
                text = "REC 00:00"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(dpToPx(4f), 0, dpToPx(8f), 0)
            }
            controls.addView(timerTv)
            timerText = timerTv

            // Pause/Resume Button
            val pauseBtn = createActionButton(
                iconRes = R.drawable.ic_overlay_pause,
                bgColor = Color.parseColor("#33FFFFFF")
            ) {
                if (isPaused) {
                    listener.onResumeClicked()
                    setPausedState(false)
                } else {
                    listener.onPauseClicked()
                    setPausedState(true)
                }
            }
            controls.addView(pauseBtn)
            pauseResumeBtn = pauseBtn.findViewById(1001)

            // Stop Button (Red)
            var isStopTriggered = false
            val stopBtn = createActionButton(
                iconRes = R.drawable.ic_overlay_stop,
                bgColor = Color.parseColor("#E50914")
            ) {
                if (isStopTriggered) return@createActionButton
                isStopTriggered = true
                listener.onStopClicked()
                bringAppToFront()
                dismiss()
            }
            controls.addView(stopBtn)

            // Return to App Button
            val appBtn = createActionButton(
                iconRes = R.drawable.ic_overlay_home,
                bgColor = Color.parseColor("#264A7F")
            ) {
                bringAppToFront()
            }
            controls.addView(appBtn)

            // Collapse Button
            val closeBtn = ImageView(context).apply {
                val size = dpToPx(24f)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(dpToPx(4f), 0, dpToPx(4f), 0)
                }
                setImageResource(R.drawable.ic_overlay_close)
                setColorFilter(Color.parseColor("#9AA0A6"))
                setOnClickListener {
                    collapse()
                }
            }
            controls.addView(closeBtn)

            rootLayout.addView(controls)
            controlsBar = controls

            overlayRootView = rootLayout
            windowManager.addView(rootLayout, layoutParams)
            isAttached = true

            // Start timer ticks
            elapsedSeconds = 0
            isTimerRunning = true
            mainHandler.post(timerRunnable)

            Log.d(TAG, "Floating control overlay successfully attached to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach floating overlay", e)
            isAttached = false
        }
    }

    private fun createActionButton(
        iconRes: Int,
        bgColor: Int,
        onClick: () -> Unit
    ): FrameLayout {
        val size = dpToPx(34f)
        val button = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(dpToPx(4f), 0, dpToPx(4f), 0)
            }
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
            }
            background = bg
            setOnClickListener { onClick() }
        }

        val icon = ImageView(context).apply {
            id = 1001
            val iconSize = dpToPx(18f)
            val lp = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
            layoutParams = lp
            setImageResource(iconRes)
            setColorFilter(Color.WHITE)
        }
        button.addView(icon)
        return button
    }

    private fun toggleExpanded() {
        if (isExpanded) collapse() else expand()
    }

    private fun expand() {
        controlsBar?.visibility = View.VISIBLE
        isExpanded = true
    }

    private fun collapse() {
        controlsBar?.visibility = View.GONE
        isExpanded = false
    }

    fun setPausedState(paused: Boolean) {
        isPaused = paused
        mainHandler.post {
            if (paused) {
                bubbleIcon?.setImageResource(R.drawable.ic_overlay_play)
                pauseResumeBtn?.setImageResource(R.drawable.ic_overlay_play)
                timerText?.text = "PAUSED"
                timerText?.setTextColor(Color.parseColor("#38EF7D"))
            } else {
                bubbleIcon?.setImageResource(R.drawable.ic_overlay_pause)
                pauseResumeBtn?.setImageResource(R.drawable.ic_overlay_pause)
                updateTimerDisplay()
            }
        }
    }

    fun updateTimer(seconds: Long) {
        elapsedSeconds = seconds
        if (!isPaused) {
            updateTimerDisplay()
        }
    }

    private fun updateTimerDisplay() {
        val mins = elapsedSeconds / 60
        val secs = elapsedSeconds % 60
        timerText?.text = String.format("REC %02d:%02d", mins, secs)
        timerText?.setTextColor(Color.WHITE)
    }

    private fun bringAppToFront() {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening MainActivity from overlay", e)
        }
    }

    fun dismiss() {
        isTimerRunning = false
        mainHandler.removeCallbacks(timerRunnable)
        if (isAttached && overlayRootView != null) {
            try {
                windowManager.removeView(overlayRootView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view from WindowManager", e)
            } finally {
                overlayRootView = null
                isAttached = false
                isExpanded = false
            }
        }
    }
}
