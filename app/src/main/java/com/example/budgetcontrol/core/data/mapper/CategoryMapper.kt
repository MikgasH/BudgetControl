package com.example.budgetcontrol.core.data.mapper

import com.example.budgetcontrol.core.data.local.database.entities.CategoryEntity
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryType

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        iconName = iconName,
        color = color,
        isDefault = isDefault,
        type = when (type) {
            "INCOME" -> CategoryType.INCOME
            else -> CategoryType.EXPENSE
        }
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        iconName = iconName,
        color = color,
        isDefault = isDefault,
        type = type.name
    )
}

fun List<CategoryEntity>.toDomain(): List<Category> {
    return map { it.toDomain() }
}