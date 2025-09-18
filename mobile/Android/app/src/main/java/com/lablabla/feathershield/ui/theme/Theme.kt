package com.lablabla.feathershield.ui.theme
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define the Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlueLight,          // Main interactive elements (buttons, active states)
    onPrimary = TextLight,               // Text/icon color on primary
    primaryContainer = PrimaryBlueLight.copy(alpha = 0.1f), // Lighter background for primary-themed sections
    onPrimaryContainer = PrimaryBlueDark, // Text/icon color on primary container
    inversePrimary = FeatherBlue,        // Alternative primary color (e.g., for inverted themes)

    secondary = FeatherBlue,             // Secondary interactive elements / accents
    onSecondary = TextLight,             // Text/icon color on secondary
    secondaryContainer = FeatherBlue.copy(alpha = 0.1f), // Lighter background for secondary-themed sections
    onSecondaryContainer = PrimaryBlueDark, // Text/icon color on secondary container

    tertiary = SilverAccent,             // Additional accent color
    onTertiary = TextDark,               // Text/icon color on tertiary
    tertiaryContainer = SilverAccent.copy(alpha = 0.1f), // Lighter background for tertiary-themed sections
    onTertiaryContainer = TextDark,      // Text/icon color on tertiary container

    background = Grey50,                 // Main screen background
    onBackground = TextDark,             // Text/icon color on background

    surface = Color.White,               // Cards, sheets, dialogs
    onSurface = TextDark,                // Text/icon color on surface
    surfaceVariant = Grey200,            // Alternative surface color (e.g., divider, light borders)
    onSurfaceVariant = Grey800,          // Text/icon color on surface variant

    error = ErrorRed,                    // Error states
    onError = TextLight,                 // Text/icon color on error
    errorContainer = ErrorRed.copy(alpha = 0.1f), // Lighter error background
    onErrorContainer = ErrorRed,         // Text/icon color on error container

    outline = SilverAccent,              // Outlines for fields, buttons, etc.
    outlineVariant = Grey200,            // Lighter outline
    scrim = Color.Black.copy(alpha = 0.3f), // Used for modal overlays

    // The Material 3 guide also includes inverseSurface and inverseOnSurface for toggled themes
    inverseSurface = Grey900,
    inverseOnSurface = TextLight,
)

// Define the Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = TextLight,
    primaryContainer = PrimaryBlueLight.copy(alpha = 0.2f),
    onPrimaryContainer = TextLight,
    inversePrimary = FeatherBlue,

    secondary = FeatherBlue,
    onSecondary = TextLight,
    secondaryContainer = FeatherBlue.copy(alpha = 0.2f),
    onSecondaryContainer = TextLight,

    tertiary = SilverAccent,
    onTertiary = TextLight,
    tertiaryContainer = SilverAccent.copy(alpha = 0.2f),
    onTertiaryContainer = TextLight,

    background = Grey900,
    onBackground = TextLight,

    surface = Grey800,
    onSurface = TextLight,
    surfaceVariant = Grey800,
    onSurfaceVariant = Grey200,

    error = ErrorRed,
    onError = TextLight,
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = ErrorRed,

    outline = SilverAccent,
    outlineVariant = Grey800,
    scrim = Color.Black.copy(alpha = 0.5f),

    inverseSurface = Grey50,
    inverseOnSurface = TextDark,
)

@Composable
fun FeatherShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}