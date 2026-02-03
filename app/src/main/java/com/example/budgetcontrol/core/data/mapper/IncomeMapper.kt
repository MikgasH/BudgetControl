package com.example.budgetcontrol.core.data.mapper

import com.example.budgetcontrol.core.data.local.database.entities.IncomeEntity
import com.example.budgetcontrol.core.domain.model.Income

fun IncomeEntity.toDomain(): Income {
    return Income(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt
    )
}

fun Income.toEntity(): IncomeEntity {
    return IncomeEntity(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt
    )
}

fun List<IncomeEntity>.toDomain(): List<Income> {
    return map { it.toDomain() }
}