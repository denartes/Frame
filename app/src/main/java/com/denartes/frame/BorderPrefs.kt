package com.denartes.frame

import android.content.Context
import android.content.SharedPreferences
import android.util.TypedValue
import androidx.core.content.edit

object BorderPrefs {
    private const val PREFS_NAME  = "border_settings"
    private const val KEY_COLOR   = "color"
    private const val KEY_THICK   = "thickness"
    private const val KEY_RADIUS  = "corner_radius"
    private const val KEY_OPACITY = "opacity"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(
        context: Context,
        colorArgb: Int,
        thicknessDp: Float,
        cornerRadiusDp: Float,
        opacity: Float
    ) {
        prefs(context).edit {
            putInt(KEY_COLOR,   colorArgb)
            putFloat(KEY_THICK,   thicknessDp)
            putFloat(KEY_RADIUS,  cornerRadiusDp)
            putFloat(KEY_OPACITY, opacity)
        }
    }

    fun colorArgb(context: Context): Int     = prefs(context).getInt(KEY_COLOR,   0xFFFFFF00.toInt())
    fun thicknessDp(context: Context): Float  = prefs(context).getFloat(KEY_THICK,   6f)
    fun radiusDp(context: Context): Float     = prefs(context).getFloat(KEY_RADIUS,  24f)
    fun opacity(context: Context): Float      = prefs(context).getFloat(KEY_OPACITY, 1f)

    fun thicknessPx(context: Context): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, thicknessDp(context), context.resources.displayMetrics)

    fun radiusPx(context: Context): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radiusDp(context), context.resources.displayMetrics)
}
