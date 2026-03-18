package com.example.budgetcontrol.core.domain.model

/**
 * Объединенная модель для расходов и доходов
 */
sealed class Transaction {
    abstract val id: String
    abstract val amount: Double
    abstract val categoryId: String
    abstract val description: String?
    abstract val date: Long
    abstract val createdAt: Long
    abstract val type: TransactionType

    data class ExpenseTransaction(
        override val id: String,
        override val amount: Double,
        override val categoryId: String,
        override val description: String?,
        override val date: Long,
        override val createdAt: Long,
        val originalAmount: Double = amount,
        val originalCurrency: String = "EUR",
        val bankName: String? = null,
        val bankCommission: Double? = null,
        val rateSource: String? = null
    ) : Transaction() {
        override val type: TransactionType = TransactionType.EXPENSE
    }

    data class IncomeTransaction(
        override val id: String,
        override val amount: Double,
        override val categoryId: String,
        override val description: String?,
        override val date: Long,
        override val createdAt: Long,
        val originalAmount: Double = amount,
        val originalCurrency: String = "EUR",
        val bankName: String? = null,
        val bankCommission: Double? = null,
        val rateSource: String? = null
    ) : Transaction() {
        override val type: TransactionType = TransactionType.INCOME
    }
}

/**
 * Тип транзакции
 */
enum class TransactionType {
    EXPENSE,
    INCOME;

    fun isExpense() = this == EXPENSE
    fun isIncome() = this == INCOME
}

/**
 * Конвертация из старых моделей в новую
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
        bankName = bankName,
        bankCommission = bankCommission,
        rateSource = rateSource
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
        bankName = bankName,
        bankCommission = bankCommission,
        rateSource = rateSource
    )
}

/**
 * Конвертация обратно в старые модели (для совместимости)
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
        bankName = bankName,
        bankCommission = bankCommission,
        rateSource = rateSource
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
        bankName = bankName,
        bankCommission = bankCommission,
        rateSource = rateSource
    )
}