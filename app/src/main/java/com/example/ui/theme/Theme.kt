package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val VoltOledColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = OledBlack,
    primaryContainer = NeonCyanDim,
    onPrimaryContainer = NeonCyan,
    secondary = NeonEmerald,
    onSecondary = OledBlack,
    secondaryContainer = NeonEmeraldDim,
    onSecondaryContainer = NeonEmerald,
    tertiary = NeonAmber,
    onTertiary = OledBlack,
    tertiaryContainer = NeonAmberDim,
    onTertiaryContainer = NeonAmber,
    error = NeonCoral,
    onError = TextPrimary,
    errorContainer = NeonCoralDim,
    onErrorContainer = NeonCoral,
    background = OledBlack,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = OledBlack,
    surfaceContainerLow = CarbonSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ElevatedSlate,
    surfaceContainerHighest = ActiveBorder,
    outline = HairlineBorder,
    outlineVariant = ActiveBorder,
)

private val VoltShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun VoltTorrentTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = VoltOledColorScheme,
        typography = VoltTypography,
        shapes = VoltShapes,
        content = content
    )
}
