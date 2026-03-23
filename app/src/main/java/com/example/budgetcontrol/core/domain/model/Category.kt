package com.example.budgetcontrol.core.domain.model

data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val color: String,
    val isDefault: Boolean = false,
    val type: CategoryType = CategoryType.EXPENSE,
    val nameKey: String? = null,
    val isSystem: Boolean = false,
    val usageCount: Int = 0
)

enum class CategoryType {
    EXPENSE,
    INCOME
}

fun List<Category>.findById(categoryId: String): Category? =
    find { it.id == categoryId }