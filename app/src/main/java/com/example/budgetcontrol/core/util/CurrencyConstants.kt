package com.example.budgetcontrol.core.util

import java.util.Currency
import java.util.Locale

const val DEFAULT_BASE_CURRENCY = "EUR"

fun getCurrencySymbol(currencyCode: String): String {
    return try {
        Currency.getInstance(currencyCode).symbol
    } catch (_: Exception) {
        currencyCode
    }
}

fun formatAmount(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", amount)
    }
}
