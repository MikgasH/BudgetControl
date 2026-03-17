package com.example.budgetcontrol.core.domain.model

import java.util.Locale

data class Expense(
    val id: String,
    val amount: Double,
    val categoryId: String,
    val description: String?,
    val date: Long,
    val createdAt: Long,
    val originalAmount: Double = amount,
    val originalCurrency: String = "EUR",
    val exchangeRate: Double? = null,
    val bankName: String? = null,
    val bankCommission: Double? = null,
    val rateSource: String? = null
) {

    val wasConverted: Boolean
        get() = originalCurrency != "EUR" && exchangeRate != null


    fun getDisplayAmount(): String {
        return if (wasConverted) {
            "${String.format(Locale.US, "%.2f", amount)} € (${String.format(Locale.US, "%.2f", originalAmount)} $originalCurrency)"
        } else {
            "${String.format(Locale.US, "%.2f", amount)} €"
        }
    }
}