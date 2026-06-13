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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BorderOuter
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denartes.frame.ui.theme.FrameTheme
import kotlin.math.roundToInt

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
                    onSettingsChange = { startForegroundService(OverlayService.updateIntent(this)) }
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
    var frameEnabled   by remember { mutableStateOf(false) }
    var borderColor    by remember { mutableStateOf(Color(BorderPrefs.colorArgb(context))) }
    var thickness      by remember { mutableFloatStateOf(BorderPrefs.thicknessDp(context)) }
    var cornerRadius   by remember { mutableFloatStateOf(BorderPrefs.radiusDp(context)) }
    var opacity        by remember { mutableFloatStateOf(BorderPrefs.opacity(context)) }

    fun saveAndNotify() {
        BorderPrefs.save(context, borderColor.toArgb(), thickness, cornerRadius, opacity)
        if (frameEnabled) onSettingsChange()
    }
    var glowEnabled    by remember { mutableStateOf(true) }
    var glowColor      by remember { mutableStateOf(Color(0xFF00BCD4)) }
    var glowStrength   by remember { mutableFloatStateOf(0.70f) }
    var glowBlur       by remember { mutableFloatStateOf(25f) }
    var glowSpread     by remember { mutableFloatStateOf(10f) }
    var glowOpacity    by remember { mutableFloatStateOf(0.80f) }

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
                        BorderPrefs.save(context, borderColor.toArgb(), thickness, cornerRadius, opacity)
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
                    color    = borderColor
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
                SettingsDivider()
                SliderRow(
                    title    = "Opacity",
                    subtitle = "Border transparency",
                    value    = opacity,
                    onValueChange = { opacity = it; saveAndNotify() },
                    valueRange = 0f..1f,
                    unit  = "%",
                    displayValue = { "${(it * 100).roundToInt()}%" }
                )
            }

            // ── Glow settings ───────────────────────────────────────────────
            SectionHeader("GLOW SETTINGS", Icons.Default.AutoAwesome)
            SettingsCard {
                ToggleRow(
                    title    = "Glow",
                    subtitle = "Enable glow effect",
                    checked  = glowEnabled,
                    onCheckedChange = { glowEnabled = it }
                )
                SettingsDivider()
                ColorRow(
                    title    = "Glow color",
                    subtitle = "Glow color",
                    color    = glowColor
                )
                SettingsDivider()
                SliderRow(
                    title    = "Glow strength",
                    subtitle = "Intensity of the glow",
                    value    = glowStrength,
                    onValueChange = { glowStrength = it },
                    valueRange = 0f..1f,
                    unit  = "%",
                    displayValue = { "${(it * 100).roundToInt()}%" }
                )
                SettingsDivider()
                SliderRow(
                    title    = "Glow blur",
                    subtitle = "Softness of the glow",
                    value    = glowBlur,
                    onValueChange = { glowBlur = it },
                    valueRange = 0f..100f,
                    unit  = "px",
                    steps = 99
                )
                SettingsDivider()
                SliderRow(
                    title    = "Glow spread",
                    subtitle = "How far the glow extends",
                    value    = glowSpread,
                    onValueChange = { glowSpread = it },
                    valueRange = 0f..50f,
                    unit  = "px",
                    steps = 49
                )
                SettingsDivider()
                SliderRow(
                    title    = "Glow opacity",
                    subtitle = "Glow transparency",
                    value    = glowOpacity,
                    onValueChange = { glowOpacity = it },
                    valueRange = 0f..1f,
                    unit  = "%",
                    displayValue = { "${(it * 100).roundToInt()}%" }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
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
private fun ColorRow(title: String, subtitle: String, color: Color) {
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
                    .clickable { }
            )
        }
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SettingRowBase(
        title    = title,
        subtitle = subtitle,
        trailing = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}


