package com.example.budgetcontrol.core.data.mapper

import com.example.budgetcontrol.core.data.local.database.entities.CurrencyExchangeEntity
import com.example.budgetcontrol.core.domain.model.CurrencyExchange

fun CurrencyExchangeEntity.toDomain(): CurrencyExchange {
    return CurrencyExchange(
        id = id,
        fromAmount = fromAmount,
        fromCurrency = fromCurrency,
        toAmount = toAmount,
        toCurrency = toCurrency,
        exchangeRate = exchangeRate,
        location = location,
        date = date,
        createdAt = createdAt
    )
}

fun CurrencyExchange.toEntity(): CurrencyExchangeEntity {
    return CurrencyExchangeEntity(
        id = id,
        fromAmount = fromAmount,
        fromCurrency = fromCurrency,
        toAmount = toAmount,
        toCurrency = toCurrency,
        exchangeRate = exchangeRate,
        location = location,
        date = date,
        createdAt = createdAt
    )
}

fun List<CurrencyExchangeEntity>.toDomain(): List<CurrencyExchange> {
    return map { it.toDomain() }
}
