package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = OnElectricCyanContainer,
    secondary = ComplianceAmber,
    onSecondary = OnComplianceAmber,
    secondaryContainer = ComplianceAmberContainer,
    onSecondaryContainer = OnComplianceAmberContainer,
    tertiary = RecordingCrimson,
    onTertiary = OnRecordingCrimson,
    tertiaryContainer = RecordingCrimsonContainer,
    onTertiaryContainer = OnRecordingCrimsonContainer,
    background = MidnightBg,
    onBackground = Color(0xFFEAF2FF),
    surface = MidnightSurface,
    onSurface = Color(0xFFEAF2FF),
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = Color(0xFFAEC0DA),
    outline = Color(0xFF364B6B)
)

private val LightColorScheme = lightColorScheme(
    primary = DeepTealPrimary,
    onPrimary = OnDeepTealPrimary,
    primaryContainer = DeepTealContainer,
    onPrimaryContainer = OnDeepTealContainer,
    secondary = WarmAmberSecondary,
    onSecondary = OnWarmAmberSecondary,
    secondaryContainer = WarmAmberContainer,
    onSecondaryContainer = OnWarmAmberContainer,
    tertiary = CrimsonTertiary,
    onTertiary = Color.White,
    tertiaryContainer = CrimsonTertiaryContainer,
    onTertiaryContainer = Color(0xFF40000D),
    background = SlateLightBg,
    onBackground = Color(0xFF0E1826),
    surface = SlateLightSurface,
    onSurface = Color(0xFF0E1826),
    surfaceVariant = SlateLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF44546A),
    outline = Color(0xFFB5C4D8)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to rich Studio Midnight dark theme for audio waveform clarity
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
