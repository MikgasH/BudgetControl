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
        createdAt = createdAt,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        exchangeRate = exchangeRate,
        bankName = bankName,
        bankCommission = bankCommission,
        rateSource = rateSource
    )
}

fun Income.toEntity(): IncomeEntity {
    return IncomeEntity(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        exchangeRate = exchangeRate,
        bankName = bankName,
        bankCommission = bankCommission,
        rateSource = rateSource
    )
}

fun List<IncomeEntity>.toDomain(): List<Income> {
    return map { it.toDomain() }
}
