package com.example.budgetcontrol.core.data.remote.cerps.dto

import java.math.BigDecimal

data class ConversionResponse(
    val originalAmount: BigDecimal,
    val fromCurrency: String,
    val toCurrency: String,
    val convertedAmount: BigDecimal,
    val exchangeRate: BigDecimal,
    val timestamp: String?
)