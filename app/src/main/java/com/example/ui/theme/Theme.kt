package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MrStudyDarkColorScheme = darkColorScheme(
    primary = NeonPurplePrimary,
    onPrimary = OnNeonPurple,
    primaryContainer = NeonPurpleContainer,
    onPrimaryContainer = OnNeonPurpleContainer,
    secondary = ElectricBlueSecondary,
    onSecondary = OnElectricBlue,
    secondaryContainer = ElectricBlueContainer,
    onSecondaryContainer = OnElectricBlueContainer,
    tertiary = CyberCyanTertiary,
    onTertiary = OnCyberCyan,
    tertiaryContainer = CyberCyanContainer,
    onTertiaryContainer = OnCyberCyanContainer,
    background = SpaceBlackBackground,
    onBackground = SpaceTextPrimary,
    surface = SpaceGlassSurface,
    onSurface = SpaceTextPrimary,
    surfaceVariant = SpaceGlassVariant,
    onSurfaceVariant = SpaceTextSecondary,
    outline = SpaceGlassOutline,
    error = DangerRed,
    onError = OnNeonPurple
)

private val MrStudyLightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = OnNavyPrimary,
    primaryContainer = NavyContainer,
    onPrimaryContainer = OnNavyContainer,
    secondary = AmberSecondary,
    onSecondary = OnAmberSecondary,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    tertiary = TealTertiary,
    onTertiary = OnTealTertiary,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealContainer,
    background = ParchmentBackground,
    onBackground = OnParchment,
    surface = WarmSurface,
    onSurface = OnParchment,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = OnWarmSurfaceVariant,
    outline = WarmOutline,
    error = DangerRed
)

val MrStudyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MrStudyTheme(
    themeMode: String = "dark",
    content: @Composable () -> Unit
) {
    // MrStudy 3.0 defaults to the futuristic Space Black / Neon Purple dark look unless user picks "light"
    val isDark = themeMode != "light"
    val colorScheme = if (isDark) MrStudyDarkColorScheme else MrStudyLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MrStudyTypography,
        shapes = MrStudyShapes,
        content = content
    )
}
