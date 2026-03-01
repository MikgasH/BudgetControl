package com.example.budgetcontrol.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,

    secondary = DarkSecondary,
    onSecondary = Color.White,
    tertiary = DarkTertiary,
    onTertiary = Color.White,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainer,
    surfaceContainerHighest = DarkSurfaceContainer,
    surfaceContainerLow = DarkSurface,
    surfaceContainerLowest = DarkBackground,

    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,

    error = DarkError,
    onError = Color.White,
    errorContainer = Color(0xFF3B1A1A),
    onErrorContainer = DarkError,

    inverseSurface = DarkOnBackground,
    inverseOnSurface = DarkBackground,
    inversePrimary = AppBlueDark,

    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = AppBlue,
    secondary = AppBlueDark,
    tertiary = AppBlue,
    background = AppWhite,
    surface = AppWhite,
    error = AppError,
    onPrimary = AppWhite,
    onSecondary = AppWhite,
    onTertiary = AppWhite
)

@Composable
fun BudgetControlTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
