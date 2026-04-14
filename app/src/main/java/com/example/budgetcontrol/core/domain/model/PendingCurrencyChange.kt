package com.example.budgetcontrol.core.domain.model

data class PendingCurrencyChange(
    val accountId: String,
    val name: String,
    val iconName: String,
    val color: String,
    val fromCurrency: String,
    val toCurrency: String,
    val oldInitialBalance: Double,
    val newInitialBalance: Double,
    val exchangeRate: Double?
)
