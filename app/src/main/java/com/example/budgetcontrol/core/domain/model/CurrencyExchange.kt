package com.example.budgetcontrol.core.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class CurrencyExchange(
    val id: String,
    val fromAmount: Double,
    val fromCurrency: String,
    val toAmount: Double,
    val toCurrency: String,
    val exchangeRate: Double,
    val location: String? = null,
    val date: Long,
    val createdAt: Long
)
