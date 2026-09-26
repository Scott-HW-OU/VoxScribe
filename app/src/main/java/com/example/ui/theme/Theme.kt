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
    primary = NeonPurple,
    onPrimary = OnNeonPurple,
    primaryContainer = NeonPurpleContainer,
    onPrimaryContainer = OnNeonPurpleContainer,
    secondary = NeonGreen,
    onSecondary = OnNeonGreen,
    secondaryContainer = NeonGreenContainer,
    onSecondaryContainer = OnNeonGreenContainer,
    tertiary = RecordingCrimson,
    onTertiary = OnRecordingCrimson,
    tertiaryContainer = RecordingCrimsonContainer,
    onTertiaryContainer = OnRecordingCrimsonContainer,
    background = MidnightBg,
    onBackground = Color(0xFFF6EEFF),
    surface = MidnightSurface,
    onSurface = Color(0xFFF6EEFF),
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = Color(0xFFCDB8EB),
    outline = Color(0xFF59388A)
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
    onTertiaryContainer = Color(0xFF400018),
    background = SlateLightBg,
    onBackground = Color(0xFF170B29),
    surface = SlateLightSurface,
    onSurface = Color(0xFF170B29),
    surfaceVariant = SlateLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF4E386E),
    outline = Color(0xFFC4ADE6)
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
    darkTheme: Boolean = true,
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
