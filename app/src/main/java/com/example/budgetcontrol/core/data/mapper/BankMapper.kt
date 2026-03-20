package com.example.budgetcontrol.core.data.mapper

import com.example.budgetcontrol.core.data.local.database.entities.BankEntity
import com.example.budgetcontrol.core.domain.model.Bank

fun BankEntity.toDomain(): Bank {
    return Bank(
        id = id,
        name = name,
        commissionPercent = commissionPercent,
        isDefault = isDefault,
        isFavorite = isFavorite
    )
}

fun Bank.toEntity(): BankEntity {
    return BankEntity(
        id = id,
        name = name,
        commissionPercent = commissionPercent,
        isDefault = isDefault,
        isFavorite = isFavorite
    )
}

fun List<BankEntity>.toDomain(): List<Bank> {
    return map { it.toDomain() }
}
