package com.example.budgetcontrol.core.domain.usecase

import android.content.Context
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.remote.FirestoreExpenseRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SyncDataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localRepository: ExpenseRepository,
    private val remoteRepository: FirestoreExpenseRepository
) {

    suspend fun backupToCloud(): Result<String> {
        return try {
            val localExpenses = localRepository.getAllExpenses().first()
            val success = remoteRepository.saveAllExpenses(localExpenses)

            if (success) {
                Result.success(context.getString(R.string.backup_success, localExpenses.size))
            } else {
                Result.failure(Exception(context.getString(R.string.backup_create_error)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloud(): Result<String> {
        return try {
            val remoteExpenses = remoteRepository.getAllExpenses()
            val localExpensesCount = localRepository.getAllExpenses().first().size

            remoteExpenses.forEach { expense ->
                localRepository.insertExpense(expense)
            }

            val newLocalCount = localRepository.getAllExpenses().first().size
            val actuallyRestored = newLocalCount - localExpensesCount

            Result.success(context.getString(R.string.sync_complete, remoteExpenses.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncExpenseToCloud(expenseId: String): Result<Unit> {
        return try {
            val expense = localRepository.getExpenseById(expenseId)
            if (expense != null) {
                val success = remoteRepository.saveExpense(expense)
                if (success) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(context.getString(R.string.sync_error)))
                }
            } else {
                Result.failure(Exception(context.getString(R.string.expense_not_found)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Получить количество трат в облаке (для отображения статуса)
    suspend fun getCloudExpensesCount(): Int {
        return try {
            remoteRepository.getAllExpenses().size
        } catch (e: Exception) {
            0
        }
    }
}