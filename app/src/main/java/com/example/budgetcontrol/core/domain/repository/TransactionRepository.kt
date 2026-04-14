package com.example.budgetcontrol.core.domain.repository

import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType

/**
 * Unified facade for expense/income operations. Callers pass a [TransactionType]
 * and receive/emit [Transaction] subtypes, letting feature code avoid branching
 * on expense vs income at every call site.
 */
interface TransactionRepository {
    suspend fun insert(transaction: Transaction)
    suspend fun getById(id: String, type: TransactionType): Transaction?
    suspend fun deleteById(id: String, type: TransactionType)
    suspend fun delete(transaction: Transaction)
}
