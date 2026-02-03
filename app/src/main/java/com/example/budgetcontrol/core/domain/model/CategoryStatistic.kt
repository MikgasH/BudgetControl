package com.example.budgetcontrol.core.domain.model

data class CategoryStatistic(
    val category: Category,
    val totalAmount: Double,
    val percentage: Float,
    val expenseCount: Int
)