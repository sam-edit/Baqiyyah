package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAppIsDark = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF135A46),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EFE6),
    onPrimaryContainer = Color(0xFF052A20),
    secondary = Color(0xFF1D775E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF3F1),
    onSecondaryContainer = Color(0xFF2D312E),
    tertiary = Color(0xFFC59419),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF7E3),
    onTertiaryContainer = Color(0xFF2D312E),
    background = Color(0xFFF7F9F8),
    onBackground = Color(0xFF2D312E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2D312E),
    surfaceVariant = Color(0xFFEFF3F1),
    onSurfaceVariant = Color(0xFF5F6964),
    outline = Color(0xFFDEE5E1)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4A2),
    onPrimary = Color(0xFF022017),
    primaryContainer = Color(0xFF134235),
    onPrimaryContainer = Color(0xFFC7F9E9),
    secondary = Color(0xFF4EE2B7),
    onSecondary = Color(0xFF022017),
    secondaryContainer = Color(0xFF1D352E),
    onSecondaryContainer = Color(0xFFD3EFE6),
    tertiary = Color(0xFFFACC15),
    onTertiary = Color(0xFF382A00),
    tertiaryContainer = Color(0xFF3E310C),
    onTertiaryContainer = Color(0xFFFEF08A),
    background = Color(0xFF0F1715),
    onBackground = Color(0xFFF0F6F3),
    surface = Color(0xFF172420),
    onSurface = Color(0xFFF0F6F3),
    surfaceVariant = Color(0xFF1F302B),
    onSurfaceVariant = Color(0xFFA0B5AC),
    outline = Color(0xFF2E453E)
)

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

@Composable
fun AlDeenTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.SideEffect {
        AlDeenFontRegistry.init(context)
    }

    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalAppIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

