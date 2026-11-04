package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryNeonLime,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerLime,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryEmerald,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerEmerald,
    onSecondaryContainer = Color(0xFF00210C),
    tertiary = TertiarySaffronGold,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiarySaffronBright,
    onTertiaryContainer = Color(0xFF2A1700),
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerHighestDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorCoral
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryForest,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = Color(0xFF142900),
    secondary = SecondaryForest,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB5F7C9),
    onSecondaryContainer = Color(0xFF00210C),
    tertiary = TertiarySaffronLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF2A1700),
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerHighLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    outline = Color(0xFF6F7A73),
    outlineVariant = Color(0xFFBFC9C2)
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
        content = content
    )
}
