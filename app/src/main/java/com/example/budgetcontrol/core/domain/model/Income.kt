package com.example.budgetcontrol.core.domain.model

import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.getCurrencySymbol
import java.util.Locale

data class Income(
    val id: String,
    val amount: Double,
    val categoryId: String,
    val description: String?,
    val date: Long,
    val createdAt: Long,
    val originalAmount: Double = amount,
    val originalCurrency: String = DEFAULT_BASE_CURRENCY,
    val exchangeRate: Double? = null,
    val bankName: String? = null,
    val bankCommission: Double? = null,
    val rateSource: String? = null,
    val accountId: String? = null
) {
    fun wasConverted(baseCurrency: String): Boolean =
        originalCurrency != baseCurrency && exchangeRate != null

    fun getDisplayAmount(baseCurrency: String): String {
        return if (wasConverted(baseCurrency)) {
            "${String.format(Locale.US, "%.2f", amount)} ${getCurrencySymbol(baseCurrency)} (${String.format(Locale.US, "%.2f", originalAmount)} ${getCurrencySymbol(originalCurrency)})"
        } else {
            "${String.format(Locale.US, "%.2f", amount)} ${getCurrencySymbol(baseCurrency)}"
        }
    }
}
