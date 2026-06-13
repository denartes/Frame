package com.denartes.frame

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BorderOuter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.denartes.frame.ui.theme.FrameTheme
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // proceed regardless — notification just won't show on denial
            startForegroundService(OverlayService.startIntent(this))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrameTheme(darkTheme = true, dynamicColor = false) {
                MainScreen(
                    activity        = this,
                    onStartOverlay  = { startOverlayService() },
                    onStopOverlay   = { stopService(Intent(this, OverlayService::class.java)) },
                    onSettingsChange = {
                        // Start service if not running (preview mode), then update prefs.
                        if (Settings.canDrawOverlays(this)) {
                            startForegroundService(OverlayService.updateIntent(this))
                        }
                    }
                )
            }
        }
    }

    private fun startOverlayService() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startForegroundService(OverlayService.startIntent(this))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    activity: ComponentActivity? = null,
    onStartOverlay: () -> Unit = {},
    onStopOverlay: () -> Unit = {},
    onSettingsChange: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var frameEnabled   by remember { mutableStateOf(false) }
    var borderColor    by remember { mutableStateOf(Color(BorderPrefs.colorArgb(context))) }
    var thickness      by remember { mutableFloatStateOf(BorderPrefs.thicknessDp(context)) }
    var cornerRadius   by remember { mutableFloatStateOf(BorderPrefs.radiusDp(context)) }
    var opacity        by remember { mutableFloatStateOf(BorderPrefs.opacity(context)) }

    var glowEnabled    by remember { mutableStateOf(BorderPrefs.glowEnabled(context)) }
    var glowColor      by remember { mutableStateOf(Color(BorderPrefs.glowColorArgb(context))) }
    var glowStrength   by remember { mutableFloatStateOf(BorderPrefs.glowStrength(context)) }
    var glowBlur       by remember { mutableFloatStateOf(BorderPrefs.glowBlurDp(context)) }
    var glowSpread     by remember { mutableFloatStateOf(BorderPrefs.glowSpreadDp(context)) }
    var glowOpacity    by remember { mutableFloatStateOf(BorderPrefs.glowOpacity(context)) }

    fun saveAndNotify() {
        BorderPrefs.save(
            context, borderColor.toArgb(), thickness, cornerRadius, opacity,
            glowEnabled, glowColor.toArgb(), glowStrength, glowBlur, glowSpread, glowOpacity
        )
        onSettingsChange()
    }

    // Start a preview overlay whenever the settings screen is visible so the
    // user can see changes live without needing Frame to be "ON".
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onSettingsChange()
                Lifecycle.Event.ON_PAUSE  -> { if (!frameEnabled) onStopOverlay() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frame", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Power card ──────────────────────────────────────────────────
            PowerCard(
                enabled = frameEnabled,
                onToggle = { on ->
                    frameEnabled = on
                    if (on) {
                        BorderPrefs.save(context, borderColor.toArgb(), thickness, cornerRadius, opacity,
                            glowEnabled, glowColor.toArgb(), glowStrength, glowBlur, glowSpread, glowOpacity)
                        onStartOverlay()
                    } else {
                        onStopOverlay()
                    }
                }
            )

            // ── Border settings ─────────────────────────────────────────────
            SectionHeader("BORDER SETTINGS", Icons.Outlined.BorderOuter)
            SettingsCard {
                ColorRow(
                    title    = "Color",
                    subtitle = "Border color",
                    color    = borderColor,
                    onColorChange = { borderColor = it; saveAndNotify() }
                )
                SettingsDivider()
                SliderRow(
                    title    = "Thickness",
                    subtitle = "Border thickness",
                    value    = thickness,
                    onValueChange = { thickness = it; saveAndNotify() },
                    valueRange = 1f..50f,
                    unit  = "dp",
                    steps = 48
                )
                SettingsDivider()
                SliderRow(
                    title    = "Corner radius",
                    subtitle = "Roundness of corners",
                    value    = cornerRadius,
                    onValueChange = { cornerRadius = it; saveAndNotify() },
                    valueRange = 0f..100f,
                    unit  = "dp",
                    steps = 49
                )
            }

            // ── Glow settings ───────────────────────────────────────────────
            SectionHeader("GLOW SETTINGS", Icons.Default.AutoAwesome)
            SettingsCard {
                ToggleRow(
                    title    = "Glow",
                    subtitle = "Enable glow effect",
                    checked  = glowEnabled,
                    onCheckedChange = { glowEnabled = it; saveAndNotify() }
                )
                SettingsDivider()
                ColorRow(
                    title    = "Glow color",
                    subtitle = "Glow color",
                    color    = glowColor,
                    onColorChange = { glowColor = it; saveAndNotify() }
                )
                SettingsDivider()
                SliderRow(
                    title    = "Glow strength",
                    subtitle = "Intensity of the glow",
                    value    = glowStrength,
                    onValueChange = { glowStrength = it; saveAndNotify() },
                    valueRange = 0f..1f,
                    unit  = "%",
                    displayValue = { "${(it * 100).roundToInt()}%" }
                )
                SettingsDivider()
                SliderRow(
                    title    = "Glow blur",
                    subtitle = "Softness of the glow",
                    value    = glowBlur,
                    onValueChange = { glowBlur = it; saveAndNotify() },
                    valueRange = 0f..100f,
                    unit  = "px",
                    steps = 99
                )
                SettingsDivider()
                SliderRow(
                    title    = "Glow spread",
                    subtitle = "How far the glow extends",
                    value    = glowSpread,
                    onValueChange = { glowSpread = it; saveAndNotify() },
                    valueRange = 0f..50f,
                    unit  = "px",
                    steps = 49
                )
                SettingsDivider()
                SliderRow(
                    title    = "Glow opacity",
                    subtitle = "Glow transparency",
                    value    = glowOpacity,
                    onValueChange = { glowOpacity = it; saveAndNotify() },
                    valueRange = 0f..1f,
                    unit  = "%",
                    displayValue = { "${(it * 100).roundToInt()}%" }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

        // Preview border — always visible while the app is open
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = thickness.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(cornerRadius.dp)
                )
        )
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun PowerCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        shape     = RoundedCornerShape(16.dp),
        color     = MaterialTheme.colorScheme.surfaceContainer,
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint   = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = if (enabled) "Frame is ON" else "Frame is OFF",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 16.sp
                )
                Text(
                    text     = if (enabled) "Border is active" else "Border is inactive",
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun SectionHeader(label: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text       = label,
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize   = 12.sp,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape  = RoundedCornerShape(16.dp),
        color  = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        thickness = 0.5.dp
    )
}

@Composable
private fun SettingRowBase(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(
                subtitle,
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String = "",
    steps: Int = 0,
    displayValue: ((Float) -> String)? = null
) {
    val label = displayValue?.invoke(value)
        ?: if (unit == "%") "${(value * 100).roundToInt()}%"
        else "${value.roundToInt()} $unit"

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingRowBase(
            title    = title,
            subtitle = subtitle,
            trailing = {
                Text(
                    label,
                    color     = MaterialTheme.colorScheme.primary,
                    fontSize  = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        )
        Slider(
            value         = value,
            onValueChange = onValueChange,
            valueRange    = valueRange,
            steps         = steps,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun ColorRow(title: String, subtitle: String, color: Color, onColorChange: (Color) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    SettingRowBase(
        title    = title,
        subtitle = subtitle,
        trailing = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clickable { showPicker = true }
            )
        }
    )

    if (showPicker) {
        ColorWheelPickerDialog(
            initialColor = color,
            onDismiss    = { showPicker = false },
            onColorPicked = { picked ->
                onColorChange(picked)
                showPicker = false
            }
        )
    }
}

@Composable
private fun ColorWheelPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorPicked: (Color) -> Unit
) {
    // Decompose initial color into HSL + alpha
    val initHsl = FloatArray(3)
    ColorUtils.colorToHSL(initialColor.toArgb(), initHsl)

    var hue        by remember { mutableFloatStateOf(initHsl[0]) }          // 0..360
    var saturation by remember { mutableFloatStateOf(initHsl[1]) }          // 0..1
    // darkness: 0 = black, 1 = full color (no darkening). Maps to HSL lightness via sat.
    // Full color from wheel is lightness = lerp(0.5 * sat, pure) — we store as a 0..1 multiplier
    var darkness   by remember { mutableFloatStateOf(
        if (initHsl[1] > 0f) (initHsl[2] / (0.5f * initHsl[1])).coerceIn(0f, 1f) else 1f
    ) }  // 0=black, 1=no darkening
    var alpha      by remember { mutableFloatStateOf(initialColor.alpha) }  // 0..1

    // lightness: outer ring (sat=1) has L=0.5 at darkness=1, inner (sat=0) is white (L=1)
    // darkness scales from L=0 (black) to the "natural" lightness
    val naturalL   = 0.5f * saturation + (1f - saturation)  // = 1 - 0.5*sat
    val lightness  = naturalL * darkness
    val pickedColor = Color(
        ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
    ).copy(alpha = alpha)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Choose color", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Color wheel
                ColorWheelCanvas(
                    hue        = hue,
                    saturation = saturation,
                    onPick     = { h, s -> hue = h; saturation = s }
                )

                // Darkness slider: black → full color (no labels)
                val fullColor = Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, naturalL)))
                GradientSlider(
                    value         = darkness,
                    onValueChange = { darkness = it },
                    startColor    = Color.Black,
                    endColor      = fullColor
                )

                // Opacity slider: transparent → picked color (no labels)
                GradientSlider(
                    value         = alpha,
                    onValueChange = { alpha = it },
                    startColor    = pickedColor.copy(alpha = 0f),
                    endColor      = pickedColor.copy(alpha = 1f)
                )

                // Preview swatch
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(pickedColor)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorPicked(pickedColor) }) {
                Text("OK", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

// Sunflower-spiral color wheel drawn with Canvas dots
@Composable
private fun ColorWheelCanvas(
    hue: Float,
    saturation: Float,
    onPick: (hue: Float, saturation: Float) -> Unit
) {
    // Concentric rings: ring 0 = 1 white center dot, ring N = N*6 dots evenly spaced
    val ringCount = 7  // rings 1..7 around the white center
    val dotsPerRing = { ring: Int -> if (ring == 0) 1 else ring * 6 }

    // Snap a raw (hue°, saturation) to the nearest dot's (hue, saturation)
    fun snapToNearest(rawHue: Float, rawSat: Float, maxRadius: Float): Pair<Float, Float> {
        val ringStep = maxRadius / ringCount
        var bestHue = 0f; var bestSat = 0f; var bestDist = Float.MAX_VALUE
        for (ring in 0..ringCount) {
            val r   = ring * ringStep
            val sat = (r / maxRadius).coerceIn(0f, 1f)
            val count = dotsPerRing(ring)
            for (dot in 0 until count) {
                val dotHue = if (count == 1) 0f else (dot.toFloat() / count) * 360f
                // Distance in polar-projected Cartesian space
                val dHueRad = Math.toRadians((rawHue - dotHue).toDouble())
                val dx = rawSat * cos(Math.toRadians(rawHue.toDouble())).toFloat() -
                         sat    * cos(Math.toRadians(dotHue.toDouble())).toFloat()
                val dy = rawSat * sin(Math.toRadians(rawHue.toDouble())).toFloat() -
                         sat    * sin(Math.toRadians(dotHue.toDouble())).toFloat()
                val d = dx * dx + dy * dy
                if (d < bestDist) { bestDist = d; bestHue = dotHue; bestSat = sat }
            }
        }
        return bestHue to bestSat
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(260.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val cx = size.width  / 2f
                        val cy = size.height / 2f
                        val radius = minOf(cx, cy)
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist <= radius) {
                            val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            val rawH = ((angle + 360f) % 360f)
                            val rawS = (dist / radius).coerceIn(0f, 1f)
                            val (h, s) = snapToNearest(rawH, rawS, radius)
                            onPick(h, s)
                        }
                    }
                }
        ) {
            val cx = size.width  / 2f
            val cy = size.height / 2f
            val maxRadius = minOf(cx, cy)
            val ringStep  = maxRadius / ringCount

            for (ring in 0..ringCount) {
                val r    = ring * ringStep
                val sat  = (r / maxRadius).coerceIn(0f, 1f)
                val dotL = 1f - 0.5f * sat
                val count = dotsPerRing(ring)
                // dot radius: center=12px, grows to 18px at outer edge
                val dotRadius = 12f + sat * 6f

                for (dot in 0 until count) {
                    val angle  = if (count == 1) 0.0 else (dot.toDouble() / count) * 2.0 * Math.PI
                    val x      = cx + r * cos(angle).toFloat()
                    val y      = cy + r * sin(angle).toFloat()
                    val dotHue = if (count == 1) 0f else ((dot.toFloat() / count) * 360f)
                    val dotColor = Color(
                        ColorUtils.HSLToColor(floatArrayOf(dotHue, sat, dotL))
                    )
                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                }
            }

            // Selection indicator — drawn exactly on the snapped dot position
            val selTheta = Math.toRadians(hue.toDouble())
            val selRing  = (saturation * ringCount).roundToInt().coerceIn(0, ringCount)
            val selR     = selRing * (maxRadius / ringCount)
            val selX     = cx + selR * cos(selTheta).toFloat()
            val selY     = cy + selR * sin(selTheta).toFloat()
            val selL     = 1f - 0.5f * saturation
            drawCircle(color = Color.White, radius = 16f, center = Offset(selX, selY))
            drawCircle(
                color  = Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, selL))),
                radius = 11f,
                center = Offset(selX, selY)
            )
        }
    }
}

@Composable
private fun GradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    startColor: Color,
    endColor: Color
) {
    val thumbRadiusDp = 13.dp
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val thumbPx = thumbRadiusDp.toPx()
                    val trackLen = size.width - thumbPx * 2
                    onValueChange(((offset.x - thumbPx) / trackLen).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val thumbPx = thumbRadiusDp.toPx()
                    val trackLen = size.width - thumbPx * 2
                    onValueChange(((change.position.x - thumbPx) / trackLen).coerceIn(0f, 1f))
                }
            }
    ) {
        val trackH   = 10.dp.toPx()
        val thumbR   = thumbRadiusDp.toPx()
        val cy       = size.height / 2f
        val trackStart = thumbR
        val trackEnd   = size.width - thumbR
        val trackLen   = trackEnd - trackStart

        // Checkerboard for transparency hint on opacity slider
        if (startColor.alpha < 0.05f) {
            val cell = trackH / 2
            val cols = ((trackLen) / cell).toInt() + 1
            for (col in 0..cols) {
                val shade = if (col % 2 == 0) Color(0xFFAAAAAA) else Color(0xFFCCCCCC)
                drawRect(
                    color   = shade,
                    topLeft = Offset(trackStart + col * cell, cy - trackH / 2),
                    size    = Size(cell, trackH)
                )
            }
        }

        // Gradient track
        drawRoundRect(
            brush       = Brush.horizontalGradient(listOf(startColor, endColor), startX = trackStart, endX = trackEnd),
            topLeft     = Offset(trackStart, cy - trackH / 2),
            size        = Size(trackLen, trackH),
            cornerRadius = CornerRadius(trackH / 2)
        )

        // Thumb
        val thumbX = trackStart + value * trackLen
        drawCircle(color = Color.White,                        radius = thumbR,       center = Offset(thumbX, cy))
        drawCircle(color = Color.Black.copy(alpha = 0.25f),    radius = thumbR,       center = Offset(thumbX, cy), style = Stroke(2.5f))
        drawCircle(color = endColor.copy(alpha = 1f),          radius = thumbR * 0.55f, center = Offset(thumbX, cy))
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SettingRowBase(
        title    = title,
        subtitle = subtitle,
        trailing = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}


