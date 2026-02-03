package com.example.budgetcontrol.core.data.mapper

import com.example.budgetcontrol.core.data.local.database.entities.ExpenseEntity
import com.example.budgetcontrol.core.domain.model.Expense

fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        exchangeRate = exchangeRate
    )
}

fun Expense.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        exchangeRate = exchangeRate
    )
}

fun List<ExpenseEntity>.toDomain(): List<Expense> {
    return map { it.toDomain() }
}