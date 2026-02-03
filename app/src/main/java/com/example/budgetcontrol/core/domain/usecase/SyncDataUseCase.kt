package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.data.remote.FirestoreExpenseRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SyncDataUseCase @Inject constructor(
    private val localRepository: ExpenseRepository,
    private val remoteRepository: FirestoreExpenseRepository
) {

    // Создать backup (загрузить локальные данные в облако)
    suspend fun backupToCloud(): Result<String> {
        return try {
            val localExpenses = localRepository.getAllExpenses().first()
            val success = remoteRepository.saveAllExpenses(localExpenses)

            if (success) {
                Result.success("Backup создан успешно! Сохранено ${localExpenses.size} трат.")
            } else {
                Result.failure(Exception("Ошибка при создании backup"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Восстановить данные (загрузить из облака в локальную БД)
    suspend fun restoreFromCloud(): Result<String> {
        return try {
            val remoteExpenses = remoteRepository.getAllExpenses()
            val localExpensesCount = localRepository.getAllExpenses().first().size

            remoteExpenses.forEach { expense ->
                localRepository.insertExpense(expense)
            }

            val newLocalCount = localRepository.getAllExpenses().first().size
            val actuallyRestored = newLocalCount - localExpensesCount

            Result.success("Синхронизация завершена! Обработано ${remoteExpenses.size} трат из облака.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Синхронизировать одну трату при добавлении
    suspend fun syncExpenseToCloud(expenseId: String): Result<Unit> {
        return try {
            val expense = localRepository.getExpenseById(expenseId)
            if (expense != null) {
                val success = remoteRepository.saveExpense(expense)
                if (success) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Ошибка синхронизации"))
                }
            } else {
                Result.failure(Exception("Трата не найдена"))
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