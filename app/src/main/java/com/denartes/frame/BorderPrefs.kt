package com.denartes.frame

import android.content.Context
import android.content.SharedPreferences
import android.util.TypedValue
import androidx.core.content.edit

object BorderPrefs {
    private const val PREFS_NAME        = "border_settings"
    private const val KEY_COLOR         = "color"
    private const val KEY_THICK         = "thickness"
    private const val KEY_RADIUS        = "corner_radius"
    private const val KEY_OPACITY       = "opacity"
    private const val KEY_GLOW_ENABLED  = "glow_enabled"
    private const val KEY_GLOW_COLOR    = "glow_color"
    private const val KEY_GLOW_STRENGTH = "glow_strength"
    private const val KEY_GLOW_BLUR     = "glow_blur"
    private const val KEY_GLOW_SPREAD   = "glow_spread"
    private const val KEY_GLOW_OPACITY  = "glow_opacity"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(
        context: Context,
        colorArgb: Int,
        thicknessDp: Float,
        cornerRadiusDp: Float,
        opacity: Float,
        glowEnabled: Boolean,
        glowColorArgb: Int,
        glowStrength: Float,
        glowBlurDp: Float,
        glowSpreadDp: Float,
        glowOpacity: Float
    ) {
        prefs(context).edit {
            putInt(KEY_COLOR,          colorArgb)
            putFloat(KEY_THICK,        thicknessDp)
            putFloat(KEY_RADIUS,       cornerRadiusDp)
            putFloat(KEY_OPACITY,      opacity)
            putBoolean(KEY_GLOW_ENABLED,  glowEnabled)
            putInt(KEY_GLOW_COLOR,        glowColorArgb)
            putFloat(KEY_GLOW_STRENGTH,   glowStrength)
            putFloat(KEY_GLOW_BLUR,       glowBlurDp)
            putFloat(KEY_GLOW_SPREAD,     glowSpreadDp)
            putFloat(KEY_GLOW_OPACITY,    glowOpacity)
        }
    }

    fun colorArgb(context: Context): Int     = prefs(context).getInt(KEY_COLOR,   0xFFFFFF00.toInt())
    fun thicknessDp(context: Context): Float  = prefs(context).getFloat(KEY_THICK,   6f)
    fun radiusDp(context: Context): Float     = prefs(context).getFloat(KEY_RADIUS,  24f)
    fun opacity(context: Context): Float      = prefs(context).getFloat(KEY_OPACITY, 1f)

    fun glowEnabled(context: Context): Boolean  = prefs(context).getBoolean(KEY_GLOW_ENABLED,  false)
    fun glowColorArgb(context: Context): Int    = prefs(context).getInt(KEY_GLOW_COLOR,         0xFF00BCD4.toInt())
    fun glowStrength(context: Context): Float   = prefs(context).getFloat(KEY_GLOW_STRENGTH,    0.70f)
    fun glowBlurDp(context: Context): Float     = prefs(context).getFloat(KEY_GLOW_BLUR,        25f)
    fun glowSpreadDp(context: Context): Float   = prefs(context).getFloat(KEY_GLOW_SPREAD,      10f)
    fun glowOpacity(context: Context): Float    = prefs(context).getFloat(KEY_GLOW_OPACITY,     0.80f)

    fun thicknessPx(context: Context): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, thicknessDp(context), context.resources.displayMetrics)

    fun radiusPx(context: Context): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radiusDp(context), context.resources.displayMetrics)
}
