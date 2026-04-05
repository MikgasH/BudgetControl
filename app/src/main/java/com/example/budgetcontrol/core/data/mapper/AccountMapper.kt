package com.example.budgetcontrol.core.data.mapper

import com.example.budgetcontrol.core.data.local.database.entities.AccountEntity
import com.example.budgetcontrol.core.domain.model.Account

fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        name = name,
        iconName = iconName,
        color = color,
        initialBalance = initialBalance,
        currency = currency,
        isDefault = isDefault,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        sortOrder = sortOrder
    )
}

fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        name = name,
        iconName = iconName,
        color = color,
        initialBalance = initialBalance,
        currency = currency,
        isDefault = isDefault,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        sortOrder = sortOrder
    )
}

fun List<AccountEntity>.toDomain(): List<Account> {
    return map { it.toDomain() }
}
