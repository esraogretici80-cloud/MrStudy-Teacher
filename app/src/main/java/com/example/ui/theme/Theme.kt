package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MrStudyDarkColorScheme = darkColorScheme(
    primary = IndigoDarkPrimary,
    onPrimary = OnIndigoDarkPrimary,
    primaryContainer = IndigoDarkContainer,
    onPrimaryContainer = OnIndigoDarkContainer,
    secondary = GoldDarkSecondary,
    onSecondary = OnGoldDarkSecondary,
    secondaryContainer = GoldDarkContainer,
    onSecondaryContainer = OnGoldDarkContainer,
    tertiary = MintDarkTertiary,
    onTertiary = OnMintDarkTertiary,
    tertiaryContainer = MintDarkContainer,
    onTertiaryContainer = OnMintDarkContainer,
    background = ObsidianBackground,
    onBackground = OnObsidian,
    surface = ObsidianSurface,
    onSurface = OnObsidian,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = OnObsidianSurfaceVariant,
    outline = ObsidianOutline,
    error = DangerRed
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
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MrStudyTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) MrStudyDarkColorScheme else MrStudyLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MrStudyTypography,
        shapes = MrStudyShapes,
        content = content
    )
}
