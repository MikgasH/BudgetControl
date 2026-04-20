package com.example.budgetcontrol.ui.util

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

fun String.toSafeColor(fallback: Color = Color.Gray): Color =
    try { Color(this.toColorInt()) } catch (_: Exception) { fallback }
