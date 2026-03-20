package com.example.budgetcontrol.core.util

import java.util.Currency

const val DEFAULT_BASE_CURRENCY = "EUR"

fun getCurrencySymbol(currencyCode: String): String {
    return try {
        Currency.getInstance(currencyCode).symbol
    } catch (_: Exception) {
        currencyCode
    }
}
