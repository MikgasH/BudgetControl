package com.example.budgetcontrol.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AppBlueLight,
    secondary = AppBlue,
    tertiary = AppBlueDark,
    background = AppDarkBackground,
    surface = AppDarkSurface,
    surfaceVariant = AppDarkSurfaceVariant,
    error = AppError
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
