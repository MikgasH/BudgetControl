package com.example.budgetcontrol.core.domain.model

data class Expense(
    val id: String,
    val amount: Double,
    val categoryId: String,
    val description: String?,
    val date: Long,
    val createdAt: Long,
    val originalAmount: Double = amount,
    val originalCurrency: String = "EUR",
    val exchangeRate: Double? = null
) {

    val wasConverted: Boolean
        get() = originalCurrency != "EUR" && exchangeRate != null


    fun getDisplayAmount(): String {
        return if (wasConverted) {
            "${String.format("%.2f", amount)} € (${String.format("%.2f", originalAmount)} $originalCurrency)"
        } else {
            "${String.format("%.2f", amount)} €"
        }
    }
}