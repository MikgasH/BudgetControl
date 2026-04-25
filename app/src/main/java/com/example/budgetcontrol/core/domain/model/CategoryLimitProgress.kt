package com.example.budgetcontrol.core.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class CategoryLimitProgress(
    val limit: Double,
    val spent: Double,
    val remaining: Double,
    val fraction: Float
)
