package com.denartes.frame

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import kotlin.math.roundToInt

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private val overlayViews = mutableListOf<View>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        // Rebuild overlays on start or on settings-update intents
        overlayViews.forEach { windowManager.removeView(it) }
        overlayViews.clear()
        addOverlays()
        return START_NOT_STICKY
    }

    private fun addOverlays() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels

        val thicknessF = BorderPrefs.thicknessPx(this)
        val radiusF    = BorderPrefs.radiusPx(this)
        val colorArgb  = BorderPrefs.colorArgb(this)
        val opacity    = BorderPrefs.opacity(this)
        val thickness  = thicknessF.roundToInt().coerceAtLeast(1)
        val radius     = radiusF.roundToInt().coerceAtLeast(0)
        val cornerSize = radius * 2
        val hEdgeW     = (screenW - cornerSize).coerceAtLeast(0)
        val vEdgeH     = (screenH - cornerSize).coerceAtLeast(0)

        val alpha = (opacity * 255).roundToInt().coerceIn(0, 255)
        val color = (colorArgb and 0x00FFFFFF) or (alpha shl 24)

        // ── Straight edges ───────────────────────────────────────────────────

        addSolidEdge(w = hEdgeW,   h = thickness, x = radius,              y = 0,                   color = color)
        addSolidEdge(w = hEdgeW,   h = thickness, x = radius,              y = screenH - thickness, color = color)
        addSolidEdge(w = thickness, h = vEdgeH,   x = 0,                   y = radius,              color = color)
        addSolidEdge(w = thickness, h = vEdgeH,   x = screenW - thickness, y = radius,              color = color)

        // ── Corners ──────────────────────────────────────────────────────────────

        if (cornerSize > 0) {
            addCorner(Corner.TOP_LEFT,     radiusF, thicknessF, cornerSize, color, x = 0,                    y = 0)
            addCorner(Corner.TOP_RIGHT,    radiusF, thicknessF, cornerSize, color, x = screenW - cornerSize, y = 0)
            addCorner(Corner.BOTTOM_LEFT,  radiusF, thicknessF, cornerSize, color, x = 0,                    y = screenH - cornerSize)
            addCorner(Corner.BOTTOM_RIGHT, radiusF, thicknessF, cornerSize, color, x = screenW - cornerSize, y = screenH - cornerSize)
        }
    }

    private fun absParams(w: Int, h: Int, x: Int, y: Int): LayoutParams =
        LayoutParams(
            w, h,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSPARENT
        ).also {
            it.gravity = Gravity.TOP or Gravity.START
            it.x = x
            it.y = y
        }

    private fun addSolidEdge(w: Int, h: Int, x: Int, y: Int, color: Int) {
        val view = View(this).apply { setBackgroundColor(color) }
        windowManager.addView(view, absParams(w, h, x, y))
        overlayViews += view
    }

    private fun addCorner(
        corner: Corner, radius: Float, thickness: Float, cornerSize: Int,
        color: Int, x: Int, y: Int
    ) {
        val view = CornerView(this, corner, radius, thickness, color)
        windowManager.addView(view, absParams(cornerSize, cornerSize, x, y))
        overlayViews += view
    }

    override fun onDestroy() {
        overlayViews.forEach { windowManager.removeView(it) }
        overlayViews.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "overlay_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Overlay Service", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("Overlay active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1

        fun startIntent(context: Context): Intent =
            Intent(context, OverlayService::class.java)

        fun updateIntent(context: Context): Intent =
            Intent(context, OverlayService::class.java).also {
                it.action = "ACTION_UPDATE"
            }
    }
}

