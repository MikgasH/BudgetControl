package com.example.budgetcontrol.core.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class CategoryLimit(
    val categoryId: String,
    val amount: Double,
    val periodType: String
)
