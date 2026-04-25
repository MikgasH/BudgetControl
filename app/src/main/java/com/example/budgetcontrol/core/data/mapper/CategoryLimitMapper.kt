package com.example.budgetcontrol.core.data.mapper

import com.example.budgetcontrol.core.data.local.database.entities.CategoryLimitEntity
import com.example.budgetcontrol.core.domain.model.CategoryLimit

fun CategoryLimitEntity.toDomain(): CategoryLimit = CategoryLimit(
    categoryId = categoryId,
    amount = amount,
    periodType = periodType
)

fun List<CategoryLimitEntity>.toCategoryLimitDomain(): List<CategoryLimit> = map { it.toDomain() }
