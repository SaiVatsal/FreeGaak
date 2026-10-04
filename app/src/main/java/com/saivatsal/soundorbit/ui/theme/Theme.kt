package com.saivatsal.soundorbit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CosmicTeal,
    onPrimary = OnCosmicTeal,
    primaryContainer = CosmicTealContainer,
    onPrimaryContainer = TextHighEmphasis,
    secondary = NebulaCoral,
    onSecondary = OnNebulaCoral,
    secondaryContainer = NebulaCoralContainer,
    onSecondaryContainer = TextHighEmphasis,
    background = DarkSurface,
    onBackground = TextHighEmphasis,
    surface = DarkSurfaceVariant,
    onSurface = TextHighEmphasis,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = TextMediumEmphasis,
    outline = DarkBorder,
    error = ErrorRed,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = TextHighEmphasis
)

private val OledBlackColorScheme = darkColorScheme(
    primary = CosmicTeal,
    onPrimary = OnCosmicTeal,
    primaryContainer = CosmicTealContainer,
    onPrimaryContainer = TextHighEmphasis,
    secondary = NebulaCoral,
    onSecondary = OnNebulaCoral,
    secondaryContainer = NebulaCoralContainer,
    onSecondaryContainer = TextHighEmphasis,
    background = OledBlack,
    onBackground = TextHighEmphasis,
    surface = DarkSurface,
    onSurface = TextHighEmphasis,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMediumEmphasis,
    outline = DarkBorder,
    error = ErrorRed,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = TextHighEmphasis
)

@Composable
fun SoundOrbitTheme(
    oledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (oledBlack) OledBlackColorScheme else DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
