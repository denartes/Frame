package com.denartes.frame

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: BorderOverlayView
    private var isPaused = false

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = BorderOverlayView(this)
        overlayView.applyPrefs(this)
        windowManager.addView(overlayView, fullScreenParams())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        when (intent?.action) {
            ACTION_PAUSE -> {
                isPaused = true
                overlayView.visibility = View.INVISIBLE
                updateNotification()
            }
            ACTION_RESUME -> {
                isPaused = false
                overlayView.applyPrefs(this)
                overlayView.visibility = View.VISIBLE
                updateNotification()
            }
            ACTION_UPDATE -> {
                overlayView.applyPrefs(this)
            }
            else -> {
                // Fresh start or system restart (null intent)
                isPaused = false
                overlayView.applyPrefs(this)
                overlayView.visibility = View.VISIBLE
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Window params ─────────────────────────────────────────────────────────

    private fun fullScreenParams(): LayoutParams {
        val (w, h) = screenSize()
        return LayoutParams(
            w, h,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_NOT_TOUCHABLE or
                    LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSPARENT
        ).also {
            it.gravity = android.view.Gravity.TOP or android.view.Gravity.START
            it.x = 0
            it.y = 0
        }
    }

    @Suppress("DEPRECATION")
    private fun screenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val dm = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val channelId = "overlay_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Overlay Service", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val toggleAction  = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        val toggleLabel   = if (isPaused) "Resume" else "Pause"
        val togglePending = PendingIntent.getBroadcast(
            this, 0,
            Intent(toggleAction).also { it.setPackage(packageName) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, channelId)
            .setContentTitle(if (isPaused) "Frame overlay paused" else "Frame overlay active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, toggleLabel, togglePending).build())
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        const val ACTION_PAUSE  = "com.denartes.frame.ACTION_PAUSE"
        const val ACTION_RESUME = "com.denartes.frame.ACTION_RESUME"
        const val ACTION_UPDATE = "com.denartes.frame.ACTION_UPDATE"

        fun startIntent(context: Context): Intent =
            Intent(context, OverlayService::class.java)

        fun updateIntent(context: Context): Intent =
            Intent(context, OverlayService::class.java).also { it.action = ACTION_UPDATE }
    }
}

