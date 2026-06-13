package com.denartes.frame

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/**
 * A single full-screen transparent view that draws the border frame with an optional glow.
 * Owned by OverlayService and added to WindowManager once for the service's lifetime.
 * LAYER_TYPE_SOFTWARE is required for BlurMaskFilter to work.
 */
class BorderOverlayView(context: Context) : View(context) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    var borderColor: Int = 0xFFFFFF00.toInt()
    var thicknessPx: Float = 6f
    var radiusPx: Float = 0f

    var glowEnabled: Boolean = false
    var glowColor: Int = 0xFF00BCD4.toInt()
    var glowBlurPx: Float = 50f
    var glowSpreadPx: Float = 25f
    var glowAlpha: Int = 204

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private fun rebuildGlowPaint() {
        glowPaint.maskFilter = if (glowBlurPx > 0f)
            BlurMaskFilter(glowBlurPx, BlurMaskFilter.Blur.NORMAL)
        else
            null
        glowPaint.strokeWidth = thicknessPx + glowSpreadPx * 2f
        glowPaint.color = (glowColor and 0x00FFFFFF) or (glowAlpha shl 24)
    }

    override fun onDraw(canvas: Canvas) {
        val half = thicknessPx / 2f
        val rect = RectF(half, half, width - half, height - half)

        if (glowEnabled && glowAlpha > 0) {
            rebuildGlowPaint()
            // Draw glow slightly inward so BlurMaskFilter halos in both directions
            // but stays within the view bounds.
            val inset = glowSpreadPx / 2f
            val glowRect = RectF(half + inset, half + inset, width - half - inset, height - half - inset)
            val glowRadius = (radiusPx - inset).coerceAtLeast(0f)
            canvas.drawRoundRect(glowRect, glowRadius, glowRadius, glowPaint)
        }

        borderPaint.color = borderColor
        borderPaint.strokeWidth = thicknessPx
        canvas.drawRoundRect(rect, radiusPx, radiusPx, borderPaint)
    }

    fun applyPrefs(context: Context) {
        val alpha    = (BorderPrefs.opacity(context) * 255).toInt().coerceIn(0, 255)
        val argb     = BorderPrefs.colorArgb(context)
        borderColor  = (argb and 0x00FFFFFF) or (alpha shl 24)
        thicknessPx  = BorderPrefs.thicknessPx(context)
        radiusPx     = BorderPrefs.radiusPx(context)

        glowEnabled  = BorderPrefs.glowEnabled(context)
        glowColor    = BorderPrefs.glowColorArgb(context)
        glowAlpha    = (BorderPrefs.glowStrength(context) * BorderPrefs.glowOpacity(context) * 255)
                           .toInt().coerceIn(0, 255)
        glowBlurPx   = dp2px(context, BorderPrefs.glowBlurDp(context))
        glowSpreadPx = dp2px(context, BorderPrefs.glowSpreadDp(context))

        invalidate()
    }

    private fun dp2px(context: Context, dp: Float): Float =
        dp * context.resources.displayMetrics.density
}

