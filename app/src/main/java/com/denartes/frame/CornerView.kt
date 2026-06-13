package com.denartes.frame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

class CornerView(
    context: Context,
    private val corner: Corner,
    private val radiusPx: Float,
    private val thicknessPx: Float,
    color: Int = android.graphics.Color.YELLOW
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = thicknessPx
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = width.toFloat()   // cornerSize = radiusPx * 2
        val half = thicknessPx / 2f

        // Clip to the relevant quadrant so no drawing leaks outside this corner's area.
        val clipRect = when (corner) {
            Corner.TOP_LEFT     -> RectF(0f,    0f,    size / 2f, size / 2f)
            Corner.TOP_RIGHT    -> RectF(size / 2f, 0f,    size,      size / 2f)
            Corner.BOTTOM_LEFT  -> RectF(0f,    size / 2f, size / 2f, size)
            Corner.BOTTOM_RIGHT -> RectF(size / 2f, size / 2f, size,  size)
        }
        canvas.save()
        canvas.clipRect(clipRect)

        // Draw the full rounded-rect stroke inset by half the stroke width.
        val arcRadius = radiusPx - half
        val rect = RectF(half, half, size - half, size - half)
        canvas.drawRoundRect(rect, arcRadius, arcRadius, paint)

        canvas.restore()
    }
}
