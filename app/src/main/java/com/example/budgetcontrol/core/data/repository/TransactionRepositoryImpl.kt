package com.example.budgetcontrol.core.data.repository

import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.model.toExpense
import com.example.budgetcontrol.core.domain.model.toIncome
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.core.domain.repository.TransactionRepository
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
) : TransactionRepository {

    override suspend fun insert(transaction: Transaction) {
        when (transaction) {
            is Transaction.ExpenseTransaction -> expenseRepository.insertExpense(transaction.toExpense())
            is Transaction.IncomeTransaction -> incomeRepository.insertIncome(transaction.toIncome())
        }
    }

    override suspend fun getById(id: String, type: TransactionType): Transaction? {
        return when (type) {
            TransactionType.EXPENSE -> expenseRepository.getExpenseById(id)?.toTransaction()
            TransactionType.INCOME -> incomeRepository.getIncomeById(id)?.toTransaction()
        }
    }

    override suspend fun deleteById(id: String, type: TransactionType) {
        when (type) {
            TransactionType.EXPENSE -> expenseRepository.deleteExpenseById(id)
            TransactionType.INCOME -> incomeRepository.deleteIncomeById(id)
        }
    }

    override suspend fun delete(transaction: Transaction) {
        when (transaction) {
            is Transaction.ExpenseTransaction -> expenseRepository.deleteExpense(transaction.toExpense())
            is Transaction.IncomeTransaction -> incomeRepository.deleteIncome(transaction.toIncome())
        }
    }
}
