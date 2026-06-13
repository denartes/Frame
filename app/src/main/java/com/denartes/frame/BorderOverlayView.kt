package com.denartes.frame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/**
 * A single full-screen transparent view that draws the border frame.
 * Owned by OverlayService and added to WindowManager once for the service's lifetime.
 */
class BorderOverlayView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    var borderColor: Int = 0xFFFFFF00.toInt()
        set(value) { field = value; invalidate() }

    var thicknessPx: Float = 6f
        set(value) { field = value; paint.strokeWidth = value; invalidate() }

    var radiusPx: Float = 0f
        set(value) { field = value; invalidate() }

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = borderColor
        paint.strokeWidth = thicknessPx

        val half = thicknessPx / 2f
        val rect = RectF(half, half, width - half, height - half)
        canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)
    }

    fun applyPrefs(context: Context) {
        val alpha   = (BorderPrefs.opacity(context) * 255).toInt().coerceIn(0, 255)
        val argb    = BorderPrefs.colorArgb(context)
        borderColor  = (argb and 0x00FFFFFF) or (alpha shl 24)
        thicknessPx  = BorderPrefs.thicknessPx(context)
        radiusPx     = BorderPrefs.radiusPx(context)
    }
}
