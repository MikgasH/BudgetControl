package com.example.budgetcontrol.core.domain.model

import androidx.compose.runtime.Immutable
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY

/**
 * Unified model for expenses and incomes, allowing the main screen to display both types uniformly.
 */
@Immutable
sealed class Transaction {
    abstract val id: String
    abstract val amount: Double
    abstract val categoryId: String
    abstract val description: String?
    abstract val date: Long
    abstract val createdAt: Long
    abstract val type: TransactionType

    @Immutable
    data class ExpenseTransaction(
        override val id: String,
        override val amount: Double,
        override val categoryId: String,
        override val description: String?,
        override val date: Long,
        override val createdAt: Long,
        val originalAmount: Double = amount,
        val originalCurrency: String = DEFAULT_BASE_CURRENCY,
        val exchangeRate: Double? = null,
        val bankName: String? = null,
        val bankCommission: Double? = null,
        val rateSource: String? = null,
        val accountId: String? = null
    ) : Transaction() {
        override val type: TransactionType = TransactionType.EXPENSE
    }

    @Immutable
    data class IncomeTransaction(
        override val id: String,
        override val amount: Double,
        override val categoryId: String,
        override val description: String?,
        override val date: Long,
        override val createdAt: Long,
        val originalAmount: Double = amount,
        val originalCurrency: String = DEFAULT_BASE_CURRENCY,
        val exchangeRate: Double? = null,
        val bankName: String? = null,
        val bankCommission: Double? = null,
        val rateSource: String? = null,
        val accountId: String? = null
    ) : Transaction() {
        override val type: TransactionType = TransactionType.INCOME
    }
}

enum class TransactionType {
    EXPENSE,
    INCOME
}

/**
 * Convert legacy Expense/Income models to the unified Transaction type.
 */
fun Expense.toTransaction(): Transaction.ExpenseTransaction {
    return Transaction.ExpenseTransaction(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        exchangeRate = exchangeRate,
        bankName = bankName,
        bankCommission = bankCommission,
        rateSource = rateSource,
        accountId = accountId
    )
}

fun Income.toTransaction(): Transaction.IncomeTransaction {
    return Transaction.IncomeTransaction(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        exchangeRate = exchangeRate,
        bankName = bankName,
        bankCommission = bankCommission,
        rateSource = rateSource,
        accountId = accountId
    )
}

/**
 * Convert back to legacy models for backward compatibility with existing code.
 */
fun Transaction.ExpenseTransaction.toExpense(): Expense {
    return Expense(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        exchangeRate = exchangeRate,
        bankName = bankName,
        bankCommission = bankCommission,
        rateSource = rateSource,
        accountId = accountId
    )
}

fun Transaction.IncomeTransaction.toIncome(): Income {
    return Income(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        exchangeRate = exchangeRate,
        bankName = bankName,
        bankCommission = bankCommission,
        rateSource = rateSource,
        accountId = accountId
    )
}