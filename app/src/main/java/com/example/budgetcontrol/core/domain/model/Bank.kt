package com.example.budgetcontrol.core.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Bank(
    val id: Int = 0,
    val name: String,
    val commissionPercent: Double,
    val isDefault: Boolean = false,
    val isFavorite: Boolean = false
)
