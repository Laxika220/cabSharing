package com.example.cabsharing.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = MidnightNavyDark,
    primaryContainer = LavenderSurfaceDark,
    onPrimaryContainer = Color.White,
    secondary = AquaAccentDark,
    onSecondary = MidnightNavyDark,
    tertiary = PeachHighlightDark,
    onTertiary = MidnightNavyDark,
    background = MidnightNavyDark,
    onBackground = Color.White,
    surface = LavenderSurfaceDark,
    onSurface = Color.White,
    surfaceVariant = MidnightNavyDark,
    onSurfaceVariant = SlateGreyDark,
    outline = SlateGreyDark
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = LavenderSurface,
    onPrimaryContainer = MidnightNavy,
    secondary = AquaAccent,
    onSecondary = Color.White,
    tertiary = PeachHighlight,
    onTertiary = MidnightNavy,
    background = MistSurface,
    onBackground = MidnightNavy,
    surface = Color.White,
    onSurface = MidnightNavy,
    surfaceVariant = LavenderSurface,
    onSurfaceVariant = SlateGrey,
    outline = SlateGrey
)

@Composable
fun CabSharingTheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}