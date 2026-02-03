@file:Suppress("UNREACHABLE_CODE")

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
        override val createdAt: Long
    ) : Transaction() {
        override val type: TransactionType = TransactionType.EXPENSE
    }

    data class IncomeTransaction(
        override val id: String,
        override val amount: Double,
        override val categoryId: String,
        override val description: String?,
        override val date: Long,
        override val createdAt: Long
    ) : Transaction() {
        override val type: TransactionType = TransactionType.INCOME
    }
}

/**
 * Тип транзакции
 */
enum class TransactionType(val displayName: String) {
    EXPENSE("Расход"),
    INCOME("Доход");

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
        createdAt = createdAt
    )
}

fun Income.toTransaction(): Transaction.IncomeTransaction {
    return Transaction.IncomeTransaction(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt
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
        createdAt = createdAt
    )
}

fun Transaction.IncomeTransaction.toIncome(): Income {
    return Income(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt
    )

    /**
     * Создание новой транзакции противоположного типа с новым ID
     */
    fun Transaction.convertToOppositeType(): Transaction {
        val newId = java.util.UUID.randomUUID().toString()

        return when (this) {
            is Transaction.ExpenseTransaction -> {
                Transaction.IncomeTransaction(
                    id = newId,
                    amount = amount,
                    categoryId = categoryId, // Будет заменена на соответствующую категорию дохода
                    description = description,
                    date = date,
                    createdAt = System.currentTimeMillis()
                )
            }
            is Transaction.IncomeTransaction -> {
                Transaction.ExpenseTransaction(
                    id = newId,
                    amount = amount,
                    categoryId = categoryId, // Будет заменена на соответствующую категорию расхода
                    description = description,
                    date = date,
                    createdAt = System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * Проверка что транзакция изменила тип
     */
    fun Transaction.hasTypeChanged(newType: TransactionType): Boolean {
        return this.type != newType
    }
}