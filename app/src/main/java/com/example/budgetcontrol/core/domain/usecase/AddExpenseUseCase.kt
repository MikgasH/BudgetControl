package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import java.math.RoundingMode // Добавлен импорт
import java.util.UUID
import javax.inject.Inject

sealed class AddExpenseResult {
    object Success : AddExpenseResult()

    data class Error(val message: String) : AddExpenseResult()
}

class AddExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val cerpsRepository: CerpsRepository
) {
    suspend operator fun invoke(
        amount: Double,
        currency: String = "EUR",
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis(),
        bankName: String? = null,
        bankCommission: Double? = null
    ): AddExpenseResult {

        return try {
            val (finalAmount, exchangeRate) = if (currency == "EUR") {
                amount to null
            } else {
                when (val result = cerpsRepository.convert(currency, "EUR", amount)) {
                    is CerpsResult.Success -> {
                        val roundedAmount = result.data.convertedAmount
                            .setScale(2, RoundingMode.HALF_UP)
                            .toDouble()

                        roundedAmount to result.data.exchangeRate.toDouble()
                    }
                    is CerpsResult.Error -> {
                        return AddExpenseResult.Error(result.message)
                    }
                }
            }

            val expense = Expense(
                id = UUID.randomUUID().toString(),
                amount = finalAmount,
                categoryId = categoryId,
                description = description,
                date = date,
                createdAt = System.currentTimeMillis(),
                originalAmount = amount,
                originalCurrency = currency,
                exchangeRate = exchangeRate,
                bankName = bankName,
                bankCommission = bankCommission
            )

            repository.insertExpense(expense)
            AddExpenseResult.Success

        } catch (e: Exception) {
            AddExpenseResult.Error("Ошибка сохранения: ${e.message}")
        }
    }

    suspend fun addInEur(
        amount: Double,
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis()
    ): AddExpenseResult {
        return invoke(amount, "EUR", categoryId, description, date)
    }
}