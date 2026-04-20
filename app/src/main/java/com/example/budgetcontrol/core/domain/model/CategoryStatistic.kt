package com.example.budgetcontrol.core.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class CategoryStatistic(
    val category: Category,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int
)
