package com.example.budgetcontrol.core.domain.model

import androidx.compose.runtime.Immutable
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY

@Immutable
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
}
