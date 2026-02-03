package com.example.budgetcontrol.core.domain.model

data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val color: String,
    val isDefault: Boolean = false,
    val type: CategoryType = CategoryType.EXPENSE
)

enum class CategoryType {
    EXPENSE,  // Расходы
    INCOME    // Доходы
}